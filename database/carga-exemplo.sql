-- =====================================================================================
-- Meus Direitos · FIAP Global Solution 2026/2 (CitizenTech)
-- Carga de exemplo (OPCIONAL): poucos moradores, denúncias, votos, comentários e confirmações,
-- para as consultas do database/consultas.sql devolverem linhas num schema recém-criado.
--
-- A API já cria conteúdo de exemplo sozinha na primeira conta de cada cidade ("cidade viva"); este
-- arquivo serve para quem quer ver dados sem subir a API.
--
-- Como executar: no SQL Developer, depois do database/script.sql, abra este arquivo e use
-- "Executar Script" (F5). O ARQUIVO NÃO FAZ COMMIT: confira o resultado com a consulta do fim e
-- então execute COMMIT (para manter) ou ROLLBACK (para desfazer).
--
-- Cuidados para nada ser confundido com conta real:
--   * todos os moradores têm ST_SIMULADO = 'S' (o app mostra o selo "Exemplo" e as rotas públicas
--     de usuários os escondem) e e-mail @exemplo.com;
--   * DS_SENHA_HASH recebe o mesmo marcador que a API usa nos moradores de exemplo, que não confere
--     com nenhuma senha: ninguém faz login com estas contas;
--   * chaves geradas pelas sequences do projeto (SEQ_GS26S2_xxx.NEXTVAL), como a API faz;
--   * só INSERT, e só nas tabelas deste projeto. Se a carga já foi feita (o primeiro e-mail já
--     existe), o bloco não insere nada, então pode rodar de novo.
-- Para apagar depois: DELETE /api/simulacao (apaga todo o conteúdo com ST_SIMULADO = 'S').
-- =====================================================================================

SET DEFINE OFF;
SET SERVEROUTPUT ON;

DECLARE
  c_cidade    CONSTANT VARCHAR2(80)  := 'Campinas';
  c_uf        CONSTANT CHAR(2)       := 'SP';
  c_sem_login CONSTANT VARCHAR2(200) := 'SIMULADO:sem-login';
  ja_existe   NUMBER;
  agora       TIMESTAMP := SYSTIMESTAMP;

  u_ana    NUMBER; u_bruno NUMBER; u_carla NUMBER; u_diego NUMBER; u_elaine NUMBER;
  d_buraco NUMBER; d_poste NUMBER; d_calcada NUMBER; d_lixo NUMBER; d_praca NUMBER;

  FUNCTION novo_usuario(nome VARCHAR2, email VARCHAR2) RETURN NUMBER IS
    id NUMBER;
  BEGIN
    INSERT INTO tb_gs26s2_usuario (cd_usuario, nm_usuario, ds_email, ds_senha_hash, nm_cidade, sg_uf, dt_cadastro, st_simulado)
      VALUES (seq_gs26s2_usuario.NEXTVAL, nome, email, c_sem_login, c_cidade, c_uf, agora - INTERVAL '10' DAY, 'S')
      RETURNING cd_usuario INTO id;
    RETURN id;
  END;

  FUNCTION nova_denuncia(autor NUMBER, titulo VARCHAR2, descricao VARCHAR2, categoria VARCHAR2, situacao VARCHAR2,
                         endereco VARCHAR2, bairro VARCHAR2, imagem VARCHAR2, dias_atras NUMBER) RETURN NUMBER IS
    id NUMBER;
  BEGIN
    INSERT INTO tb_gs26s2_denuncia (cd_denuncia, cd_usuario, ds_titulo, ds_denuncia, tp_categoria, st_denuncia,
                                    ds_endereco, nm_bairro, nm_cidade, sg_uf, ds_imagem_url, dt_criacao, dt_atualizacao)
      VALUES (seq_gs26s2_denuncia.NEXTVAL, autor, titulo, descricao, categoria, situacao,
              endereco, bairro, c_cidade, c_uf, imagem,
              agora - NUMTODSINTERVAL(dias_atras, 'DAY'), agora - NUMTODSINTERVAL(dias_atras, 'DAY'))
      RETURNING cd_denuncia INTO id;
    RETURN id;
  END;

  PROCEDURE votar(usuario NUMBER, denuncia NUMBER, tipo VARCHAR2) IS
  BEGIN
    INSERT INTO tb_gs26s2_voto_denuncia (cd_usuario, cd_denuncia, tp_voto, dt_voto)
      VALUES (usuario, denuncia, tipo, agora - INTERVAL '1' DAY);
  END;

  PROCEDURE comentar(usuario NUMBER, denuncia NUMBER, texto VARCHAR2) IS
  BEGIN
    INSERT INTO tb_gs26s2_comentario (cd_comentario, cd_denuncia, cd_usuario, ds_comentario, dt_criacao)
      VALUES (seq_gs26s2_comentario.NEXTVAL, denuncia, usuario, texto, agora - INTERVAL '12' HOUR);
  END;

  PROCEDURE confirmar(usuario NUMBER, denuncia NUMBER) IS
  BEGIN
    INSERT INTO tb_gs26s2_confirmacao_resolucao (cd_usuario, cd_denuncia, dt_confirmacao)
      VALUES (usuario, denuncia, agora - INTERVAL '6' HOUR);
  END;
BEGIN
  SELECT COUNT(*) INTO ja_existe FROM tb_gs26s2_usuario WHERE ds_email = 'ana.souza@exemplo.com';
  IF ja_existe > 0 THEN
    DBMS_OUTPUT.PUT_LINE('Carga de exemplo já existe: nada foi inserido');
    RETURN;
  END IF;

  -- Moradores de exemplo
  u_ana    := novo_usuario('Ana Souza',     'ana.souza@exemplo.com');
  u_bruno  := novo_usuario('Bruno Lima',    'bruno.lima@exemplo.com');
  u_carla  := novo_usuario('Carla Mendes',  'carla.mendes@exemplo.com');
  u_diego  := novo_usuario('Diego Rocha',   'diego.rocha@exemplo.com');
  u_elaine := novo_usuario('Elaine Farias', 'elaine.farias@exemplo.com');

  -- Denúncias: duas no Centro e duas no Cambuí (para o relatório de bairros) e uma sem bairro
  d_buraco := nova_denuncia(u_ana, 'Buraco grande na pista',
      'Buraco no meio da faixa da direita, os carros desviam para a contramão.',
      'ASFALTO', 'ABERTA', 'Rua das Flores, 120', 'Centro', 'img/demo/asfalto-1.jpg', 6);
  d_poste := nova_denuncia(u_bruno, 'Poste apagado há uma semana',
      'A rua fica totalmente escura à noite perto do ponto de ônibus.',
      'ILUMINACAO', 'EM_ANDAMENTO', 'Avenida Brasil, 450', 'Centro', 'img/demo/iluminacao-1.jpg', 5);
  d_calcada := nova_denuncia(u_carla, 'Calçada sem rampa de acesso',
      'Quem usa cadeira de rodas precisa ir pela rua para atravessar a esquina.',
      'CALCADA_ACESSIBILIDADE', 'ABERTA', 'Rua Sete de Setembro, 88', 'Cambuí', 'img/demo/calcada-acessibilidade-1.jpg', 4);
  d_lixo := nova_denuncia(u_diego, 'Lixo acumulado na esquina',
      'Sacos de lixo e entulho ficaram na calçada depois da feira.',
      'LIMPEZA_URBANA', 'RESOLVIDA', 'Rua do Mercado, 15', 'Cambuí', 'img/demo/limpeza-urbana-1.jpg', 8);
  d_praca := nova_denuncia(u_elaine, 'Brinquedo quebrado na praça',
      'O balanço da praça está com a corrente solta.',
      'OUTROS', 'ABERTA', 'Praça da Matriz, s/n', NULL, NULL, 2);

  -- Votos: ninguém vota na própria denúncia (regra da API) e há um voto por pessoa em cada denúncia (PK)
  votar(u_bruno, d_buraco, 'UP');   votar(u_carla, d_buraco, 'UP');
  votar(u_diego, d_buraco, 'UP');   votar(u_elaine, d_buraco, 'UP');
  votar(u_ana, d_poste, 'UP');      votar(u_carla, d_poste, 'UP');   votar(u_diego, d_poste, 'DOWN');
  votar(u_ana, d_calcada, 'UP');    votar(u_bruno, d_calcada, 'UP');
  votar(u_ana, d_lixo, 'UP');
  votar(u_ana, d_praca, 'DOWN');

  -- Conversa do bairro
  comentar(u_bruno, d_buraco, 'Passei por aí hoje cedo, o buraco aumentou depois da chuva.');
  comentar(u_carla, d_buraco, 'Já vi dois carros com o pneu furado nesse trecho.');
  comentar(u_ana, d_poste, 'A equipe da prefeitura esteve no local ontem.');
  comentar(u_elaine, d_calcada, 'Na esquina de baixo tem o mesmo problema.');
  comentar(u_carla, d_lixo, 'Recolheram tudo hoje de manhã.');

  -- "Já foi resolvido": o autor não confirma. A do lixo tem 3 confirmações (por isso está RESOLVIDA)
  -- e a do poste tem 2 (falta uma, aparece na consulta "perto de serem resolvidas").
  confirmar(u_ana, d_lixo);    confirmar(u_bruno, d_lixo);   confirmar(u_carla, d_lixo);
  confirmar(u_ana, d_poste);   confirmar(u_carla, d_poste);

  DBMS_OUTPUT.PUT_LINE('Carga de exemplo inserida em ' || c_cidade || '/' || c_uf
      || ': 5 moradores, 5 denúncias, 11 votos, 5 comentários e 5 confirmações.');
  DBMS_OUTPUT.PUT_LINE('Nada foi gravado em definitivo ainda: confira e execute COMMIT ou ROLLBACK.');
END;
/

-- Conferência: o que a carga deixou em cada tabela
SELECT (SELECT COUNT(*) FROM tb_gs26s2_usuario WHERE ds_email LIKE '%@exemplo.com') AS qt_moradores,
       (SELECT COUNT(*) FROM tb_gs26s2_denuncia d JOIN tb_gs26s2_usuario u ON u.cd_usuario = d.cd_usuario
         WHERE u.ds_email LIKE '%@exemplo.com') AS qt_denuncias,
       (SELECT COUNT(*) FROM tb_gs26s2_voto_denuncia v JOIN tb_gs26s2_usuario u ON u.cd_usuario = v.cd_usuario
         WHERE u.ds_email LIKE '%@exemplo.com') AS qt_votos,
       (SELECT COUNT(*) FROM tb_gs26s2_comentario c JOIN tb_gs26s2_usuario u ON u.cd_usuario = c.cd_usuario
         WHERE u.ds_email LIKE '%@exemplo.com') AS qt_comentarios,
       (SELECT COUNT(*) FROM tb_gs26s2_confirmacao_resolucao r JOIN tb_gs26s2_usuario u ON u.cd_usuario = r.cd_usuario
         WHERE u.ds_email LIKE '%@exemplo.com') AS qt_confirmacoes
  FROM dual;

-- Depois de conferir, escolha um:
-- COMMIT;
-- ROLLBACK;
