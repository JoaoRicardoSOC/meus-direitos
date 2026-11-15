-- =====================================================================================
-- Meus Direitos · FIAP Global Solution 2026/2 (CitizenTech)
-- Consultas de exemplo: as perguntas que o "Painel da cidade" do app e a equipe fazem ao banco.
--
-- Só SELECT, e só nas tabelas deste projeto (TB_GS26S2_*): nada é alterado.
-- Juntas cobrem JOIN, LEFT JOIN, GROUP BY, HAVING, CASE, NVL, subconsulta escalar, subconsulta
-- correlacionada, LISTAGG e ROWNUM.
--
-- Como executar: no SQL Developer, rode uma consulta por vez (Ctrl+Enter). As que filtram por
-- cidade usam as variáveis :cidade e :uf; o SQL Developer pergunta os valores (ex.: Campinas e SP,
-- a cidade do database/carga-exemplo.sql).
--
-- Para simplificar, o filtro aqui é nm_cidade = :cidade (grafia exata). Na API a comparação ignora
-- acentos e maiúsculas, com a expressão de JdbcUtil.cidadeIgual, que tem índice próprio
-- (IX_GS26S2_DENUNCIA_CIDADE_CHAVE e IX_GS26S2_USUARIO_CIDADE_CHAVE).
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. Resumo da cidade
-- Pergunta: quantos moradores, denúncias, denúncias resolvidas e votos a cidade tem?
-- Técnica: subconsultas escalares, uma linha só.
-- -------------------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM tb_gs26s2_usuario u WHERE u.nm_cidade = :cidade AND u.sg_uf = :uf) AS qt_moradores,
  (SELECT COUNT(*) FROM tb_gs26s2_denuncia d WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf) AS qt_denuncias,
  (SELECT COUNT(*) FROM tb_gs26s2_denuncia d WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf
      AND d.st_denuncia = 'RESOLVIDA') AS qt_resolvidas,
  (SELECT COUNT(*) FROM tb_gs26s2_voto_denuncia v JOIN tb_gs26s2_denuncia d ON d.cd_denuncia = v.cd_denuncia
    WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf) AS qt_votos
FROM dual;

-- -------------------------------------------------------------------------------------
-- 2. Denúncias por categoria e situação
-- Pergunta: em cada categoria, quantas denúncias estão abertas, em andamento e resolvidas?
-- Técnica: GROUP BY + SUM com CASE (uma coluna por situação).
-- -------------------------------------------------------------------------------------
SELECT d.tp_categoria,
       COUNT(*) AS qt_total,
       SUM(CASE WHEN d.st_denuncia = 'ABERTA' THEN 1 ELSE 0 END) AS qt_abertas,
       SUM(CASE WHEN d.st_denuncia = 'EM_ANDAMENTO' THEN 1 ELSE 0 END) AS qt_andamento,
       SUM(CASE WHEN d.st_denuncia = 'RESOLVIDA' THEN 1 ELSE 0 END) AS qt_resolvidas
  FROM tb_gs26s2_denuncia d
 WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf
 GROUP BY d.tp_categoria
 ORDER BY qt_total DESC;

-- -------------------------------------------------------------------------------------
-- 3. Bairros com mais problemas
-- Pergunta: quais bairros têm 2 ou mais denúncias, e qual o saldo de votos (apoios - discordâncias)?
-- Técnica: LEFT JOIN (denúncia sem voto também conta) + GROUP BY + HAVING + NVL.
-- COUNT(DISTINCT) porque o LEFT JOIN repete a denúncia a cada voto.
-- -------------------------------------------------------------------------------------
SELECT NVL(d.nm_bairro, 'Não informado') AS bairro,
       COUNT(DISTINCT d.cd_denuncia) AS qt_total,
       NVL(SUM(CASE v.tp_voto WHEN 'UP' THEN 1 WHEN 'DOWN' THEN -1 END), 0) AS saldo_votos
  FROM tb_gs26s2_denuncia d
  LEFT JOIN tb_gs26s2_voto_denuncia v ON v.cd_denuncia = d.cd_denuncia
 WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf
 GROUP BY NVL(d.nm_bairro, 'Não informado')
HAVING COUNT(DISTINCT d.cd_denuncia) >= 2
 ORDER BY qt_total DESC, saldo_votos DESC;

-- -------------------------------------------------------------------------------------
-- 4. As 5 denúncias com mais participação
-- Pergunta: quais denúncias da cidade receberam mais votos, e quantos comentários cada uma tem?
-- Técnica: subconsulta correlacionada (comentários) + ROWNUM sobre uma subconsulta já ordenada.
-- -------------------------------------------------------------------------------------
SELECT * FROM (
  SELECT d.cd_denuncia, d.ds_titulo,
         COUNT(v.cd_usuario) AS qt_votos,
         (SELECT COUNT(*) FROM tb_gs26s2_comentario c WHERE c.cd_denuncia = d.cd_denuncia) AS qt_comentarios
    FROM tb_gs26s2_denuncia d
    LEFT JOIN tb_gs26s2_voto_denuncia v ON v.cd_denuncia = d.cd_denuncia
   WHERE d.nm_cidade = :cidade AND d.sg_uf = :uf
   GROUP BY d.cd_denuncia, d.ds_titulo
   ORDER BY COUNT(v.cd_usuario) DESC
) WHERE ROWNUM <= 5;

-- -------------------------------------------------------------------------------------
-- 5. Temas de direitos por público
-- Pergunta: quantos temas do catálogo se aplicam a cada público, e quais são?
-- Técnica: JOIN entre o tema e o seu atributo multivalorado (tags) + GROUP BY + LISTAGG.
-- -------------------------------------------------------------------------------------
SELECT t.tp_tag AS publico,
       COUNT(*) AS qt_temas,
       LISTAGG(d.ds_titulo, '; ') WITHIN GROUP (ORDER BY d.nr_ordem) AS temas
  FROM tb_gs26s2_direito d
  JOIN tb_gs26s2_direito_tag t ON t.cd_direito = d.cd_direito
 GROUP BY t.tp_tag
 ORDER BY qt_temas DESC, t.tp_tag;

-- -------------------------------------------------------------------------------------
-- 6. Denúncias perto de serem resolvidas
-- Pergunta: quais denúncias já têm 2 confirmações de "já foi resolvido"? (a 3ª resolve)
-- Técnica: JOIN com a tabela associativa + GROUP BY + HAVING COUNT(*) = 2.
-- -------------------------------------------------------------------------------------
SELECT d.cd_denuncia, d.ds_titulo, d.nm_cidade, d.sg_uf, d.st_denuncia,
       COUNT(*) AS qt_confirmacoes
  FROM tb_gs26s2_denuncia d
  JOIN tb_gs26s2_confirmacao_resolucao r ON r.cd_denuncia = d.cd_denuncia
 WHERE d.st_denuncia <> 'RESOLVIDA'
 GROUP BY d.cd_denuncia, d.ds_titulo, d.nm_cidade, d.sg_uf, d.st_denuncia
HAVING COUNT(*) = 2
 ORDER BY d.nm_cidade, d.cd_denuncia;

-- -------------------------------------------------------------------------------------
-- 7. Moradores reais e de exemplo por cidade
-- Pergunta: em cada cidade, quantas contas são de pessoas reais e quantas são da "cidade viva"?
-- Técnica: GROUP BY com duas colunas + CASE para traduzir a flag S/N.
-- -------------------------------------------------------------------------------------
SELECT u.nm_cidade, u.sg_uf,
       CASE u.st_simulado WHEN 'S' THEN 'Exemplo' ELSE 'Real' END AS tipo_conta,
       COUNT(*) AS qt_moradores
  FROM tb_gs26s2_usuario u
 GROUP BY u.nm_cidade, u.sg_uf, u.st_simulado
 ORDER BY u.nm_cidade, u.sg_uf, tipo_conta;

-- -------------------------------------------------------------------------------------
-- 8. Dicionário de dados gravado no banco
-- Pergunta: o que significa cada coluna das tabelas do projeto? (etapa 5 do script.sql)
-- Técnica: JOIN entre as visões do dicionário do Oracle; ESCAPE porque _ é curinga no LIKE.
-- -------------------------------------------------------------------------------------
SELECT c.table_name, c.column_name, c.data_type, m.comments
  FROM user_tab_columns c
  JOIN user_col_comments m ON m.table_name = c.table_name AND m.column_name = c.column_name
 WHERE c.table_name LIKE 'TB!_GS26S2!_%' ESCAPE '!'
 ORDER BY c.table_name, c.column_id;
