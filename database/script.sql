-- =====================================================================================
-- Meus Direitos · FIAP Global Solution 2026/2 (CitizenTech)
-- Script do banco de dados (Oracle): estrutura (DDL) e carga inicial do catálogo de direitos.
--
-- Como executar: no SQL Developer, abra este arquivo e use "Executar Script" (F5),
-- conectado ao schema que a API usa (db.usuario do backend/db.properties).
--
-- O SCRIPT NÃO APAGA NEM ALTERA NADA. Ele só cria o que ainda não existe:
--   * cada tabela, sequence e índice é criado apenas se ainda não houver um objeto com o mesmo nome;
--   * cada tema do catálogo de direitos é inserido apenas se ainda não houver um tema com o mesmo título.
-- Por isso pode ser executado num schema compartilhado com outros projetos e quantas vezes for preciso:
-- num schema que já tem as tabelas do projeto, não muda nada; num schema vazio, monta tudo do zero.
--
-- Objetos deste projeto (e somente estes):
--   tabelas    TB_USUARIO, TB_PERFIL_DEMOGRAFICO, TB_DENUNCIA, TB_COMENTARIO, TB_VOTO_DENUNCIA,
--              TB_CONFIRMACAO_RESOLUCAO, TB_REPORTE_COMENTARIO, TB_DIREITO, TB_DIREITO_PASSO, TB_DIREITO_TAG,
--              TB_SEMEADURA_CIDADE
--   sequences  SEQ_USUARIO, SEQ_DENUNCIA, SEQ_COMENTARIO, SEQ_DIREITO
--
-- As definições abaixo são as mesmas do schema usado no Oracle da FIAP (extraídas com DBMS_METADATA).
-- Convenções (as mesmas do back-end, em dao.jdbc.JdbcUtil):
--   * prefixos: TB_ tabela, CD_ código, DS_ descrição, NM_ nome, SG_ sigla, DT_ data,
--     TP_ tipo (enum), ST_ situação/flag, NR_ número, QT_ quantidade;
--   * booleanos em CHAR(1) 'S'/'N'; enums gravados pelo nome da constante Java, com CHECK;
--   * sem ON DELETE CASCADE: a API apaga as tabelas filhas antes, na mesma transação.
-- =====================================================================================

SET DEFINE OFF;
SET SERVEROUTPUT ON;

-- =====================================================================================
-- 1. Sequences (chaves geradas pela API com SEQ_xxx.NEXTVAL)
-- =====================================================================================

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_USUARIO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_USUARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_USUARIO já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_DENUNCIA START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_COMENTARIO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_DIREITO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_DIREITO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_DIREITO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_DIREITO já existe: mantida como está');
  END IF;
END;
/

-- =====================================================================================
-- 2. Tabelas (na ordem das chaves estrangeiras)
-- =====================================================================================

-- Conta do usuário. ST_SIMULADO = 'S' marca os moradores de exemplo da "cidade viva".
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_USUARIO
       (    CD_USUARIO NUMBER(10,0),
        NM_USUARIO VARCHAR2(100) NOT NULL,
        DS_EMAIL VARCHAR2(120) NOT NULL,
        DS_SENHA_HASH VARCHAR2(200) NOT NULL,
        NM_CIDADE VARCHAR2(80) NOT NULL,
        SG_UF CHAR(2) NOT NULL,
        DT_CADASTRO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        ST_SIMULADO CHAR(1) DEFAULT 'N' NOT NULL,
         CONSTRAINT PK_USUARIO PRIMARY KEY (CD_USUARIO),
         CONSTRAINT UK_USUARIO_EMAIL UNIQUE (DS_EMAIL),
         CONSTRAINT CK_USUARIO_SIMULADO CHECK (st_simulado IN ('S','N'))
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_USUARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_USUARIO já existe: mantida como está');
  END IF;
END;
/

-- Questionário "Meus Direitos" (opcional, no máximo um por usuário).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_PERFIL_DEMOGRAFICO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_PERFIL_DEMOGRAFICO
       (    CD_USUARIO NUMBER(10,0),
        DT_NASCIMENTO DATE,
        TP_RACA_COR VARCHAR2(15),
        TP_DEFICIENCIA VARCHAR2(15),
        TP_PERCURSO_ESCOLAR VARCHAR2(20),
        ST_BAIXA_RENDA CHAR(1),
        DT_ATUALIZACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT CK_PERFIL_RACA CHECK (tp_raca_cor IN
                             ('BRANCA','PRETA','PARDA','AMARELA','INDIGENA','NAO_INFORMADO')),
         CONSTRAINT CK_PERFIL_DEF CHECK (tp_deficiencia IN
                             ('NENHUMA','FISICA','VISUAL','AUDITIVA','INTELECTUAL','NAO_INFORMADO')),
         CONSTRAINT CK_PERFIL_ESC CHECK (tp_percurso_escolar IN
                             ('ESCOLA_PUBLICA','ESCOLA_PARTICULAR','MISTO','NAO_INFORMADO')),
         CONSTRAINT CK_PERFIL_RENDA CHECK (st_baixa_renda IN ('S','N')),
         CONSTRAINT PK_PERFIL PRIMARY KEY (CD_USUARIO),
         CONSTRAINT FK_PERFIL_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_PERFIL_DEMOGRAFICO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_PERFIL_DEMOGRAFICO já existe: mantida como está');
  END IF;
END;
/

-- Problema urbano registrado por um morador, na cidade do cadastro dele.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_DENUNCIA
       (    CD_DENUNCIA NUMBER(10,0),
        CD_USUARIO NUMBER(10,0) NOT NULL,
        DS_TITULO VARCHAR2(120) NOT NULL,
        DS_DENUNCIA VARCHAR2(1000) NOT NULL,
        TP_CATEGORIA VARCHAR2(25) NOT NULL,
        ST_DENUNCIA VARCHAR2(15) DEFAULT 'ABERTA' NOT NULL,
        DS_ENDERECO VARCHAR2(150) NOT NULL,
        NM_BAIRRO VARCHAR2(80),
        NM_CIDADE VARCHAR2(80) NOT NULL,
        SG_UF CHAR(2) NOT NULL,
        DS_IMAGEM_URL VARCHAR2(300),
        DT_CRIACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        DT_ATUALIZACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT CK_DENUNCIA_CAT CHECK (tp_categoria IN
                        ('ASFALTO','ILUMINACAO','CALCADA_ACESSIBILIDADE','SEGURANCA','LIMPEZA_URBANA','OUTROS')),
         CONSTRAINT CK_DENUNCIA_STATUS CHECK (st_denuncia IN ('ABERTA','EM_ANDAMENTO','RESOLVIDA')),
         CONSTRAINT PK_DENUNCIA PRIMARY KEY (CD_DENUNCIA),
         CONSTRAINT FK_DENUNCIA_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

-- "Conversa do bairro". DT_EDICAO só é preenchida quando o autor edita o comentário.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_COMENTARIO
       (    CD_COMENTARIO NUMBER(10,0),
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        CD_USUARIO NUMBER(10,0) NOT NULL,
        DS_COMENTARIO VARCHAR2(500) NOT NULL,
        DT_CRIACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        DT_EDICAO TIMESTAMP (6),
         CONSTRAINT PK_COMENTARIO PRIMARY KEY (CD_COMENTARIO),
         CONSTRAINT FK_COMENT_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_DENUNCIA (CD_DENUNCIA),
         CONSTRAINT FK_COMENT_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

-- Voto (UP/DOWN). A chave composta garante um voto por pessoa em cada denúncia.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_VOTO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_VOTO_DENUNCIA
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        TP_VOTO VARCHAR2(4) NOT NULL,
        DT_VOTO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT CK_VOTO_TIPO CHECK (tp_voto IN ('UP','DOWN')),
         CONSTRAINT PK_VOTO_DENUNCIA PRIMARY KEY (CD_USUARIO, CD_DENUNCIA),
         CONSTRAINT FK_VOTO_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO),
         CONSTRAINT FK_VOTO_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_DENUNCIA (CD_DENUNCIA)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_VOTO_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_VOTO_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

-- "Já foi resolvido": na 3ª confirmação de moradores diferentes, a denúncia vira RESOLVIDA.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_CONFIRMACAO_RESOLUCAO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_CONFIRMACAO_RESOLUCAO
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        DT_CONFIRMACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT PK_CONFIRMACAO_RESOLUCAO PRIMARY KEY (CD_USUARIO, CD_DENUNCIA),
         CONSTRAINT FK_CONFRES_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO),
         CONSTRAINT FK_CONFRES_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_DENUNCIA (CD_DENUNCIA)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_CONFIRMACAO_RESOLUCAO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_CONFIRMACAO_RESOLUCAO já existe: mantida como está');
  END IF;
END;
/

-- Reporte de comentário ofensivo. A chave composta garante um reporte por pessoa em cada comentário;
-- com 3 reportes o comentário deixa de aparecer na API (ComentarioService.REPORTES_PARA_OCULTAR).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_REPORTE_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_REPORTE_COMENTARIO
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_COMENTARIO NUMBER(10,0) NOT NULL,
        DT_REPORTE TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT PK_REPORTE_COMENTARIO PRIMARY KEY (CD_USUARIO, CD_COMENTARIO),
         CONSTRAINT FK_REPORTE_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_USUARIO (CD_USUARIO),
         CONSTRAINT FK_REPORTE_COMENTARIO FOREIGN KEY (CD_COMENTARIO)
          REFERENCES TB_COMENTARIO (CD_COMENTARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_REPORTE_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_REPORTE_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

-- Catálogo de temas de direitos (tela "Meus Direitos").
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_DIREITO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_DIREITO
       (    CD_DIREITO NUMBER(10,0),
        DS_TITULO VARCHAR2(120) NOT NULL,
        DS_RESUMO VARCHAR2(200) NOT NULL,
        DS_DIREITO VARCHAR2(1000) NOT NULL,
        DS_AVISO VARCHAR2(300),
        DS_BASE_LEGAL VARCHAR2(300),
        DS_LINK_OFICIAL VARCHAR2(300),
        TP_ICONE VARCHAR2(15) DEFAULT 'OUTRO' NOT NULL,
        NR_ORDEM NUMBER(3,0) DEFAULT 0 NOT NULL,
         CONSTRAINT CK_DIREITO_ICONE CHECK (tp_icone IN
                         ('TRANSPORTE','PRIORIDADE','MUNICIPIO','EDUCACAO','SAUDE','RENDA','OUTRO')),
         CONSTRAINT PK_DIREITO PRIMARY KEY (CD_DIREITO),
         CONSTRAINT UK_DIREITO_TITULO UNIQUE (DS_TITULO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_DIREITO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_DIREITO já existe: mantida como está');
  END IF;
END;
/

-- Passos "Por onde começar" de cada tema (1 a 10).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_DIREITO_PASSO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_DIREITO_PASSO
       (    CD_DIREITO NUMBER(10,0) NOT NULL,
        NR_ORDEM NUMBER(2,0) NOT NULL,
        DS_TITULO VARCHAR2(80) NOT NULL,
        DS_PASSO VARCHAR2(400) NOT NULL,
         CONSTRAINT CK_PASSO_ORDEM CHECK (nr_ordem BETWEEN 1 AND 10),
         CONSTRAINT PK_DIREITO_PASSO PRIMARY KEY (CD_DIREITO, NR_ORDEM),
         CONSTRAINT FK_PASSO_DIREITO FOREIGN KEY (CD_DIREITO)
          REFERENCES TB_DIREITO (CD_DIREITO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_DIREITO_PASSO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_DIREITO_PASSO já existe: mantida como está');
  END IF;
END;
/

-- Públicos a que o tema se aplica. TODOS = qualquer pessoa que respondeu o questionário.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_DIREITO_TAG';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_DIREITO_TAG
       (    CD_DIREITO NUMBER(10,0) NOT NULL,
        TP_TAG VARCHAR2(20) NOT NULL,
         CONSTRAINT CK_DTAG_TAG CHECK (tp_tag IN
                    ('IDOSO','JOVEM','PCD','PCD_MOBILIDADE','PPI','ESCOLA_PUBLICA','BAIXA_RENDA','TODOS')),
         CONSTRAINT PK_DIREITO_TAG PRIMARY KEY (CD_DIREITO, TP_TAG),
         CONSTRAINT FK_DTAG_DIREITO FOREIGN KEY (CD_DIREITO)
          REFERENCES TB_DIREITO (CD_DIREITO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_DIREITO_TAG');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_DIREITO_TAG já existe: mantida como está');
  END IF;
END;
/

-- "Cidade viva": cidades que já receberam o conteúdo de demonstração e o último pulso de atividade.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_SEMEADURA_CIDADE';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_SEMEADURA_CIDADE
       (    NM_CIDADE_CHAVE VARCHAR2(80) NOT NULL,
        SG_UF CHAR(2) NOT NULL,
        NM_CIDADE VARCHAR2(80) NOT NULL,
        DT_SEMEADURA TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        DT_ULTIMO_PULSO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        QT_EVENTOS NUMBER(6,0) DEFAULT 0 NOT NULL,
         CONSTRAINT PK_SEMEADURA_CIDADE PRIMARY KEY (NM_CIDADE_CHAVE, SG_UF)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_SEMEADURA_CIDADE');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_SEMEADURA_CIDADE já existe: mantida como está');
  END IF;
END;
/

-- =====================================================================================
-- 3. Índices (as chaves primárias e únicas já criam os seus)
-- =====================================================================================

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_COMENTARIO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_COMENTARIO_DENUNCIA ON TB_COMENTARIO (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_COMENTARIO_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_COMENTARIO_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_COMENTARIO_USUARIO ON TB_COMENTARIO (CD_USUARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_COMENTARIO_USUARIO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_CONFRES_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_CONFRES_DENUNCIA ON TB_CONFIRMACAO_RESOLUCAO (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_CONFRES_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_DENUNCIA_CIDADE';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_DENUNCIA_CIDADE ON TB_DENUNCIA (NM_CIDADE, SG_UF)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_DENUNCIA_CIDADE');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_DENUNCIA_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_DENUNCIA_USUARIO ON TB_DENUNCIA (CD_USUARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_DENUNCIA_USUARIO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_DIREITO_TAG';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_DIREITO_TAG ON TB_DIREITO_TAG (TP_TAG)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_DIREITO_TAG');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_USUARIO_SIMULADO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_USUARIO_SIMULADO ON TB_USUARIO (ST_SIMULADO, SG_UF)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_USUARIO_SIMULADO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_VOTO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_VOTO_DENUNCIA ON TB_VOTO_DENUNCIA (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_VOTO_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_REPORTE_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_REPORTE_COMENTARIO ON TB_REPORTE_COMENTARIO (CD_COMENTARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_REPORTE_COMENTARIO');
  END IF;
END;
/


-- =====================================================================================
-- 4. Carga inicial: catálogo de direitos
-- =====================================================================================
-- Linguagem cautelosa: cada tema indica uma possibilidade a verificar e o caminho oficial.
-- O tema aparece em GET /direitos/meus quando uma das suas tags bate com o perfil da pessoa
-- (TODOS aparece para qualquer perfil respondido). Cada tema só entra se ainda não houver um
-- tema com o mesmo título; temas já cadastrados não são alterados.

-- 1. Transporte e mobilidade
DECLARE
  existe  NUMBER;
  id_tema NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM tb_direito WHERE UPPER(ds_titulo) = UPPER(q'[Transporte e mobilidade]');
  IF existe = 0 THEN
    INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_direito.NEXTVAL, q'[Transporte e mobilidade]',
              q'[Saiba onde consultar gratuidades e programas de transporte.]',
              q'[Passe Livre e outros programas de transporte. Exemplos para pesquisa.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              q'[Lei 10.741/2003 (Estatuto da Pessoa Idosa), arts. 39 e 40; Lei 8.899/1994 (Passe Livre interestadual)]',
              q'[https://www.gov.br/pt-br/servicos/obter-o-passe-livre-interestadual]', 'TRANSPORTE', 1)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Identifique o programa]', q'[Verifique se o deslocamento é municipal, intermunicipal ou interestadual.]');
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Consulte o órgão responsável]', q'[Procure o portal oficial do serviço de transporte e confira os critérios atuais.]');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD_MOBILIDADE');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Transporte e mobilidade]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Transporte e mobilidade já existe: mantido como está]');
  END IF;
END;
/

-- 2. Atendimento prioritário
DECLARE
  existe  NUMBER;
  id_tema NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM tb_direito WHERE UPPER(ds_titulo) = UPPER(q'[Atendimento prioritário]');
  IF existe = 0 THEN
    INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_direito.NEXTVAL, q'[Atendimento prioritário]',
              q'[Entenda como consultar as regras de atendimento.]',
              q'[Informação para ajudar você a solicitar atendimento.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              q'[Lei 10.048/2000 (atendimento prioritário)]',
              q'[https://www.planalto.gov.br/ccivil_03/leis/l10048.htm]', 'PRIORIDADE', 2)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Entenda as regras]', q'[Consulte a legislação aplicável e as orientações oficiais do serviço.]');
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Verifique a documentação]', q'[Confira com o estabelecimento ou órgão quais comprovantes são necessários.]');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Atendimento prioritário]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Atendimento prioritário já existe: mantido como está]');
  END IF;
END;
/

-- 3. Benefícios municipais
DECLARE
  existe  NUMBER;
  id_tema NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM tb_direito WHERE UPPER(ds_titulo) = UPPER(q'[Benefícios municipais]');
  IF existe = 0 THEN
    INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_direito.NEXTVAL, q'[Benefícios municipais]',
              q'[Consulte possíveis isenções e apoios na sua cidade.]',
              q'[Isenção de IPTU para idosos e outros apoios podem depender de regras locais.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              NULL,
              NULL, 'MUNICIPIO', 3)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Consulte sua prefeitura]', q'[Busque o tema no portal oficial da cidade, sem presumir que o benefício existe.]');
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Confira os critérios]', q'[Idade, renda, imóvel e outros requisitos precisam ser verificados no município.]');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'TODOS');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Benefícios municipais]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Benefícios municipais já existe: mantido como está]');
  END IF;
END;
/

-- 4. Educação e cotas
DECLARE
  existe  NUMBER;
  id_tema NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM tb_direito WHERE UPPER(ds_titulo) = UPPER(q'[Educação e cotas]');
  IF existe = 0 THEN
    INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_direito.NEXTVAL, q'[Educação e cotas]',
              q'[Veja como funcionam as cotas no ensino superior público.]',
              q'[Universidades e institutos federais reservam vagas para quem estudou em escola pública, com subcotas para pessoas pretas, pardas, indígenas e com deficiência.]',
              q'[Cada processo seletivo publica suas próprias regras no edital. Leia o edital antes de se inscrever.]',
              q'[Lei 12.711/2012 (Lei de Cotas)]',
              q'[https://www.gov.br/mec/pt-br]', 'EDUCACAO', 4)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Confira seu percurso escolar]', q'[As cotas exigem, em geral, o ensino médio completo em escola pública.]');
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Leia o edital]', q'[No Sisu ou no vestibular, verifique as modalidades de cota e os documentos exigidos.]');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'ESCOLA_PUBLICA');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PPI');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Educação e cotas]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Educação e cotas já existe: mantido como está]');
  END IF;
END;
/

-- 5. Assistência e renda
DECLARE
  existe  NUMBER;
  id_tema NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM tb_direito WHERE UPPER(ds_titulo) = UPPER(q'[Assistência e renda]');
  IF existe = 0 THEN
    INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_direito.NEXTVAL, q'[Assistência e renda]',
              q'[Programas sociais que podem apoiar famílias de baixa renda.]',
              q'[Benefícios como o BPC e a Tarifa Social de Energia dependem do Cadastro Único e de critérios de renda.]',
              q'[A aprovação depende de análise do órgão responsável. Procure o CRAS da sua região.]',
              q'[Lei 8.742/1993 (LOAS); Lei 12.212/2010 (Tarifa Social de Energia)]',
              q'[https://www.gov.br/mds/pt-br]', 'RENDA', 5)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Atualize o Cadastro Único]', q'[Procure o CRAS com documentos de todos da família para fazer ou atualizar o cadastro.]');
    INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Pergunte sobre os programas]', q'[No atendimento, peça orientação sobre os benefícios para os quais sua família pode ser avaliada.]');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'BAIXA_RENDA');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Assistência e renda]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Assistência e renda já existe: mantido como está]');
  END IF;
END;
/

COMMIT;
