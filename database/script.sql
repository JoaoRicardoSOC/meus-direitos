-- =====================================================================================
-- Meus Direitos · FIAP Global Solution 2026/2 (CitizenTech)
-- Script do banco de dados (Oracle): estrutura (DDL) e carga inicial do catálogo de direitos.
--
-- Como executar: no SQL Developer, abra este arquivo e use "Executar Script" (F5),
-- conectado ao schema que a API usa (db.usuario do backend/db.properties).
--
-- O SCRIPT NÃO APAGA DADOS NEM OBJETOS. Ele faz só duas coisas:
--   * etapa 0: num schema criado com os nomes antigos (TB_USUARIO, SEQ_USUARIO, PK_USUARIO...), renomeia
--     cada objeto deste projeto para o padrão GS26S2 (RENAME mantém dados, chaves e índices);
--   * etapas 1 a 4: cria cada tabela, sequence e índice apenas se ainda não houver um objeto com o mesmo
--     nome, e insere cada tema do catálogo apenas se ainda não houver um tema com o mesmo título.
-- Por isso pode ser executado num schema compartilhado com outros projetos e quantas vezes for preciso:
-- num schema que já está no padrão, não muda nada; num schema vazio, monta tudo do zero.
--
-- Padrão de nomes: todo objeto deste projeto leva GS26S2 (Global Solution 2026, 2º semestre) depois do
-- prefixo do tipo, para identificar o projeto no schema e evitar conflito com outros trabalhos:
--   TB_GS26S2_<TABELA>, SEQ_GS26S2_<TABELA>, PK_GS26S2_<TABELA>, FK_GS26S2_<TABELA>_<REFERENCIADA>,
--   UK_GS26S2_<TABELA>_<COLUNA>, CK_GS26S2_<TABELA>_<COLUNA>, IX_GS26S2_<TABELA>_<COLUNA>.
-- As colunas não levam o prefixo (CD_USUARIO, DS_EMAIL...).
--
-- Objetos deste projeto (e somente estes):
--   tabelas    TB_GS26S2_USUARIO, TB_GS26S2_PERFIL_DEMOGRAFICO, TB_GS26S2_DENUNCIA, TB_GS26S2_COMENTARIO, TB_GS26S2_VOTO_DENUNCIA,
--              TB_GS26S2_CONFIRMACAO_RESOLUCAO, TB_GS26S2_REPORTE_COMENTARIO, TB_GS26S2_DIREITO, TB_GS26S2_DIREITO_PASSO, TB_GS26S2_DIREITO_TAG,
--              TB_GS26S2_SEMEADURA_CIDADE
--   sequences  SEQ_GS26S2_USUARIO, SEQ_GS26S2_DENUNCIA, SEQ_GS26S2_COMENTARIO, SEQ_GS26S2_DIREITO
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
-- 0. Migração para o padrão de nomes GS26S2
-- Só renomeia objetos deste projeto que ainda tenham o nome antigo (e cujo nome novo ainda não exista).
-- Nenhum DROP, nenhum dado apagado. Num schema novo ou já migrado, cada linha só é ignorada.
-- =====================================================================================

DECLARE
  PROCEDURE renomear_tabela(antigo VARCHAR2, novo VARCHAR2) IS
    n_antigo NUMBER; n_novo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n_antigo FROM user_tables WHERE table_name = antigo;
    SELECT COUNT(*) INTO n_novo FROM user_tables WHERE table_name = novo;
    IF n_antigo = 1 AND n_novo = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || antigo || ' RENAME TO ' || novo;
      DBMS_OUTPUT.PUT_LINE('Tabela ' || antigo || ' renomeada para ' || novo);
    END IF;
  END;

  PROCEDURE renomear_sequence(antigo VARCHAR2, novo VARCHAR2) IS
    n_antigo NUMBER; n_novo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n_antigo FROM user_sequences WHERE sequence_name = antigo;
    SELECT COUNT(*) INTO n_novo FROM user_sequences WHERE sequence_name = novo;
    IF n_antigo = 1 AND n_novo = 0 THEN
      EXECUTE IMMEDIATE 'RENAME ' || antigo || ' TO ' || novo;
      DBMS_OUTPUT.PUT_LINE('Sequence ' || antigo || ' renomeada para ' || novo);
    END IF;
  END;

  PROCEDURE renomear_restricao(tabela VARCHAR2, antigo VARCHAR2, novo VARCHAR2) IS
    n_antigo NUMBER; n_novo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n_antigo FROM user_constraints WHERE table_name = tabela AND constraint_name = antigo;
    SELECT COUNT(*) INTO n_novo FROM user_constraints WHERE constraint_name = novo;
    IF n_antigo = 1 AND n_novo = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || tabela || ' RENAME CONSTRAINT ' || antigo || ' TO ' || novo;
      DBMS_OUTPUT.PUT_LINE('Restrição ' || antigo || ' renomeada para ' || novo);
    END IF;
  END;

  -- Também usado para os índices que o Oracle criou junto com as PK e UK (eles têm o nome da restrição)
  PROCEDURE renomear_indice(tabela VARCHAR2, antigo VARCHAR2, novo VARCHAR2) IS
    n_antigo NUMBER; n_novo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO n_antigo FROM user_indexes WHERE table_name = tabela AND index_name = antigo;
    SELECT COUNT(*) INTO n_novo FROM user_indexes WHERE index_name = novo;
    IF n_antigo = 1 AND n_novo = 0 THEN
      EXECUTE IMMEDIATE 'ALTER INDEX ' || antigo || ' RENAME TO ' || novo;
      DBMS_OUTPUT.PUT_LINE('Índice ' || antigo || ' renomeado para ' || novo);
    END IF;
  END;
BEGIN
  -- Tabelas
  renomear_tabela('TB_USUARIO', 'TB_GS26S2_USUARIO');
  renomear_tabela('TB_PERFIL_DEMOGRAFICO', 'TB_GS26S2_PERFIL_DEMOGRAFICO');
  renomear_tabela('TB_DENUNCIA', 'TB_GS26S2_DENUNCIA');
  renomear_tabela('TB_COMENTARIO', 'TB_GS26S2_COMENTARIO');
  renomear_tabela('TB_VOTO_DENUNCIA', 'TB_GS26S2_VOTO_DENUNCIA');
  renomear_tabela('TB_CONFIRMACAO_RESOLUCAO', 'TB_GS26S2_CONFIRMACAO_RESOLUCAO');
  renomear_tabela('TB_REPORTE_COMENTARIO', 'TB_GS26S2_REPORTE_COMENTARIO');
  renomear_tabela('TB_DIREITO', 'TB_GS26S2_DIREITO');
  renomear_tabela('TB_DIREITO_PASSO', 'TB_GS26S2_DIREITO_PASSO');
  renomear_tabela('TB_DIREITO_TAG', 'TB_GS26S2_DIREITO_TAG');
  renomear_tabela('TB_SEMEADURA_CIDADE', 'TB_GS26S2_SEMEADURA_CIDADE');
  -- Sequences
  renomear_sequence('SEQ_USUARIO', 'SEQ_GS26S2_USUARIO');
  renomear_sequence('SEQ_DENUNCIA', 'SEQ_GS26S2_DENUNCIA');
  renomear_sequence('SEQ_COMENTARIO', 'SEQ_GS26S2_COMENTARIO');
  renomear_sequence('SEQ_DIREITO', 'SEQ_GS26S2_DIREITO');
  -- Restrições (PK, UK, FK, CHECK)
  renomear_restricao('TB_GS26S2_USUARIO', 'PK_USUARIO', 'PK_GS26S2_USUARIO');
  renomear_restricao('TB_GS26S2_USUARIO', 'UK_USUARIO_EMAIL', 'UK_GS26S2_USUARIO_EMAIL');
  renomear_restricao('TB_GS26S2_USUARIO', 'CK_USUARIO_SIMULADO', 'CK_GS26S2_USUARIO_SIMULADO');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'PK_PERFIL', 'PK_GS26S2_PERFIL_DEMOGRAFICO');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'FK_PERFIL_USUARIO', 'FK_GS26S2_PERFIL_DEMOGRAFICO_USUARIO');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'CK_PERFIL_RACA', 'CK_GS26S2_PERFIL_DEMOGRAFICO_RACA_COR');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'CK_PERFIL_DEF', 'CK_GS26S2_PERFIL_DEMOGRAFICO_DEFICIENCIA');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'CK_PERFIL_ESC', 'CK_GS26S2_PERFIL_DEMOGRAFICO_PERCURSO_ESCOLAR');
  renomear_restricao('TB_GS26S2_PERFIL_DEMOGRAFICO', 'CK_PERFIL_RENDA', 'CK_GS26S2_PERFIL_DEMOGRAFICO_BAIXA_RENDA');
  renomear_restricao('TB_GS26S2_DENUNCIA', 'PK_DENUNCIA', 'PK_GS26S2_DENUNCIA');
  renomear_restricao('TB_GS26S2_DENUNCIA', 'FK_DENUNCIA_USUARIO', 'FK_GS26S2_DENUNCIA_USUARIO');
  renomear_restricao('TB_GS26S2_DENUNCIA', 'CK_DENUNCIA_CAT', 'CK_GS26S2_DENUNCIA_CATEGORIA');
  renomear_restricao('TB_GS26S2_DENUNCIA', 'CK_DENUNCIA_STATUS', 'CK_GS26S2_DENUNCIA_STATUS');
  renomear_restricao('TB_GS26S2_COMENTARIO', 'PK_COMENTARIO', 'PK_GS26S2_COMENTARIO');
  renomear_restricao('TB_GS26S2_COMENTARIO', 'FK_COMENT_DENUNCIA', 'FK_GS26S2_COMENTARIO_DENUNCIA');
  renomear_restricao('TB_GS26S2_COMENTARIO', 'FK_COMENT_USUARIO', 'FK_GS26S2_COMENTARIO_USUARIO');
  renomear_restricao('TB_GS26S2_VOTO_DENUNCIA', 'PK_VOTO_DENUNCIA', 'PK_GS26S2_VOTO_DENUNCIA');
  renomear_restricao('TB_GS26S2_VOTO_DENUNCIA', 'FK_VOTO_USUARIO', 'FK_GS26S2_VOTO_DENUNCIA_USUARIO');
  renomear_restricao('TB_GS26S2_VOTO_DENUNCIA', 'FK_VOTO_DENUNCIA', 'FK_GS26S2_VOTO_DENUNCIA_DENUNCIA');
  renomear_restricao('TB_GS26S2_VOTO_DENUNCIA', 'CK_VOTO_TIPO', 'CK_GS26S2_VOTO_DENUNCIA_TIPO');
  renomear_restricao('TB_GS26S2_CONFIRMACAO_RESOLUCAO', 'PK_CONFIRMACAO_RESOLUCAO', 'PK_GS26S2_CONFIRMACAO_RESOLUCAO');
  renomear_restricao('TB_GS26S2_CONFIRMACAO_RESOLUCAO', 'FK_CONFRES_USUARIO', 'FK_GS26S2_CONFIRMACAO_RESOLUCAO_USUARIO');
  renomear_restricao('TB_GS26S2_CONFIRMACAO_RESOLUCAO', 'FK_CONFRES_DENUNCIA', 'FK_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA');
  renomear_restricao('TB_GS26S2_REPORTE_COMENTARIO', 'PK_REPORTE_COMENTARIO', 'PK_GS26S2_REPORTE_COMENTARIO');
  renomear_restricao('TB_GS26S2_REPORTE_COMENTARIO', 'FK_REPORTE_USUARIO', 'FK_GS26S2_REPORTE_COMENTARIO_USUARIO');
  renomear_restricao('TB_GS26S2_REPORTE_COMENTARIO', 'FK_REPORTE_COMENTARIO', 'FK_GS26S2_REPORTE_COMENTARIO_COMENTARIO');
  renomear_restricao('TB_GS26S2_DIREITO', 'PK_DIREITO', 'PK_GS26S2_DIREITO');
  renomear_restricao('TB_GS26S2_DIREITO', 'UK_DIREITO_TITULO', 'UK_GS26S2_DIREITO_TITULO');
  renomear_restricao('TB_GS26S2_DIREITO', 'CK_DIREITO_ICONE', 'CK_GS26S2_DIREITO_ICONE');
  renomear_restricao('TB_GS26S2_DIREITO_PASSO', 'PK_DIREITO_PASSO', 'PK_GS26S2_DIREITO_PASSO');
  renomear_restricao('TB_GS26S2_DIREITO_PASSO', 'FK_PASSO_DIREITO', 'FK_GS26S2_DIREITO_PASSO_DIREITO');
  renomear_restricao('TB_GS26S2_DIREITO_PASSO', 'CK_PASSO_ORDEM', 'CK_GS26S2_DIREITO_PASSO_ORDEM');
  renomear_restricao('TB_GS26S2_DIREITO_TAG', 'PK_DIREITO_TAG', 'PK_GS26S2_DIREITO_TAG');
  renomear_restricao('TB_GS26S2_DIREITO_TAG', 'FK_DTAG_DIREITO', 'FK_GS26S2_DIREITO_TAG_DIREITO');
  renomear_restricao('TB_GS26S2_DIREITO_TAG', 'CK_DTAG_TAG', 'CK_GS26S2_DIREITO_TAG_TAG');
  renomear_restricao('TB_GS26S2_SEMEADURA_CIDADE', 'PK_SEMEADURA_CIDADE', 'PK_GS26S2_SEMEADURA_CIDADE');
  -- Índices das PK e UK (mesmo nome da restrição)
  renomear_indice('TB_GS26S2_USUARIO', 'PK_USUARIO', 'PK_GS26S2_USUARIO');
  renomear_indice('TB_GS26S2_USUARIO', 'UK_USUARIO_EMAIL', 'UK_GS26S2_USUARIO_EMAIL');
  renomear_indice('TB_GS26S2_PERFIL_DEMOGRAFICO', 'PK_PERFIL', 'PK_GS26S2_PERFIL_DEMOGRAFICO');
  renomear_indice('TB_GS26S2_DENUNCIA', 'PK_DENUNCIA', 'PK_GS26S2_DENUNCIA');
  renomear_indice('TB_GS26S2_COMENTARIO', 'PK_COMENTARIO', 'PK_GS26S2_COMENTARIO');
  renomear_indice('TB_GS26S2_VOTO_DENUNCIA', 'PK_VOTO_DENUNCIA', 'PK_GS26S2_VOTO_DENUNCIA');
  renomear_indice('TB_GS26S2_CONFIRMACAO_RESOLUCAO', 'PK_CONFIRMACAO_RESOLUCAO', 'PK_GS26S2_CONFIRMACAO_RESOLUCAO');
  renomear_indice('TB_GS26S2_REPORTE_COMENTARIO', 'PK_REPORTE_COMENTARIO', 'PK_GS26S2_REPORTE_COMENTARIO');
  renomear_indice('TB_GS26S2_DIREITO', 'PK_DIREITO', 'PK_GS26S2_DIREITO');
  renomear_indice('TB_GS26S2_DIREITO', 'UK_DIREITO_TITULO', 'UK_GS26S2_DIREITO_TITULO');
  renomear_indice('TB_GS26S2_DIREITO_PASSO', 'PK_DIREITO_PASSO', 'PK_GS26S2_DIREITO_PASSO');
  renomear_indice('TB_GS26S2_DIREITO_TAG', 'PK_DIREITO_TAG', 'PK_GS26S2_DIREITO_TAG');
  renomear_indice('TB_GS26S2_SEMEADURA_CIDADE', 'PK_SEMEADURA_CIDADE', 'PK_GS26S2_SEMEADURA_CIDADE');
  -- Demais índices
  renomear_indice('TB_GS26S2_USUARIO', 'IX_USUARIO_SIMULADO', 'IX_GS26S2_USUARIO_SIMULADO');
  renomear_indice('TB_GS26S2_USUARIO', 'IX_USUARIO_CIDADE_CHAVE', 'IX_GS26S2_USUARIO_CIDADE_CHAVE');
  renomear_indice('TB_GS26S2_DENUNCIA', 'IX_DENUNCIA_CIDADE', 'IX_GS26S2_DENUNCIA_CIDADE');
  renomear_indice('TB_GS26S2_DENUNCIA', 'IX_DENUNCIA_CIDADE_CHAVE', 'IX_GS26S2_DENUNCIA_CIDADE_CHAVE');
  renomear_indice('TB_GS26S2_DENUNCIA', 'IX_DENUNCIA_USUARIO', 'IX_GS26S2_DENUNCIA_USUARIO');
  renomear_indice('TB_GS26S2_COMENTARIO', 'IX_COMENTARIO_DENUNCIA', 'IX_GS26S2_COMENTARIO_DENUNCIA');
  renomear_indice('TB_GS26S2_COMENTARIO', 'IX_COMENTARIO_USUARIO', 'IX_GS26S2_COMENTARIO_USUARIO');
  renomear_indice('TB_GS26S2_VOTO_DENUNCIA', 'IX_VOTO_DENUNCIA', 'IX_GS26S2_VOTO_DENUNCIA_DENUNCIA');
  renomear_indice('TB_GS26S2_CONFIRMACAO_RESOLUCAO', 'IX_CONFRES_DENUNCIA', 'IX_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA');
  renomear_indice('TB_GS26S2_REPORTE_COMENTARIO', 'IX_REPORTE_COMENTARIO', 'IX_GS26S2_REPORTE_COMENTARIO_COMENTARIO');
  renomear_indice('TB_GS26S2_DIREITO_TAG', 'IX_DIREITO_TAG', 'IX_GS26S2_DIREITO_TAG_TAG');
END;
/


-- =====================================================================================
-- 1. Sequences (chaves geradas pela API com SEQ_xxx.NEXTVAL)
-- =====================================================================================

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_GS26S2_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_GS26S2_USUARIO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_GS26S2_USUARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_GS26S2_USUARIO já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_GS26S2_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_GS26S2_DENUNCIA START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_GS26S2_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_GS26S2_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_GS26S2_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_GS26S2_COMENTARIO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_GS26S2_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_GS26S2_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_sequences WHERE sequence_name = 'SEQ_GS26S2_DIREITO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE SEQ_GS26S2_DIREITO START WITH 1 INCREMENT BY 1 NOCACHE';
    DBMS_OUTPUT.PUT_LINE('Criada a sequence SEQ_GS26S2_DIREITO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SEQ_GS26S2_DIREITO já existe: mantida como está');
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
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_USUARIO
       (    CD_USUARIO NUMBER(10,0),
        NM_USUARIO VARCHAR2(100) NOT NULL,
        DS_EMAIL VARCHAR2(120) NOT NULL,
        DS_SENHA_HASH VARCHAR2(200) NOT NULL,
        NM_CIDADE VARCHAR2(80) NOT NULL,
        SG_UF CHAR(2) NOT NULL,
        DT_CADASTRO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        ST_SIMULADO CHAR(1) DEFAULT 'N' NOT NULL,
         CONSTRAINT PK_GS26S2_USUARIO PRIMARY KEY (CD_USUARIO),
         CONSTRAINT UK_GS26S2_USUARIO_EMAIL UNIQUE (DS_EMAIL),
         CONSTRAINT CK_GS26S2_USUARIO_SIMULADO CHECK (st_simulado IN ('S','N'))
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_USUARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_USUARIO já existe: mantida como está');
  END IF;
END;
/

-- Questionário "Meus Direitos" (opcional, no máximo um por usuário).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_PERFIL_DEMOGRAFICO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_PERFIL_DEMOGRAFICO
       (    CD_USUARIO NUMBER(10,0),
        DT_NASCIMENTO DATE,
        TP_RACA_COR VARCHAR2(15),
        TP_DEFICIENCIA VARCHAR2(15),
        TP_PERCURSO_ESCOLAR VARCHAR2(20),
        ST_BAIXA_RENDA CHAR(1),
        DT_ATUALIZACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT CK_GS26S2_PERFIL_DEMOGRAFICO_RACA_COR CHECK (tp_raca_cor IN
                             ('BRANCA','PRETA','PARDA','AMARELA','INDIGENA','NAO_INFORMADO')),
         CONSTRAINT CK_GS26S2_PERFIL_DEMOGRAFICO_DEFICIENCIA CHECK (tp_deficiencia IN
                             ('NENHUMA','FISICA','VISUAL','AUDITIVA','INTELECTUAL','NAO_INFORMADO')),
         CONSTRAINT CK_GS26S2_PERFIL_DEMOGRAFICO_PERCURSO_ESCOLAR CHECK (tp_percurso_escolar IN
                             ('ESCOLA_PUBLICA','ESCOLA_PARTICULAR','MISTO','NAO_INFORMADO')),
         CONSTRAINT CK_GS26S2_PERFIL_DEMOGRAFICO_BAIXA_RENDA CHECK (st_baixa_renda IN ('S','N')),
         CONSTRAINT PK_GS26S2_PERFIL_DEMOGRAFICO PRIMARY KEY (CD_USUARIO),
         CONSTRAINT FK_GS26S2_PERFIL_DEMOGRAFICO_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_PERFIL_DEMOGRAFICO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_PERFIL_DEMOGRAFICO já existe: mantida como está');
  END IF;
END;
/

-- Problema urbano registrado por um morador, na cidade do cadastro dele.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_DENUNCIA
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
         CONSTRAINT CK_GS26S2_DENUNCIA_CATEGORIA CHECK (tp_categoria IN
                        ('ASFALTO','ILUMINACAO','CALCADA_ACESSIBILIDADE','SEGURANCA','LIMPEZA_URBANA','OUTROS')),
         CONSTRAINT CK_GS26S2_DENUNCIA_STATUS CHECK (st_denuncia IN ('ABERTA','EM_ANDAMENTO','RESOLVIDA')),
         CONSTRAINT PK_GS26S2_DENUNCIA PRIMARY KEY (CD_DENUNCIA),
         CONSTRAINT FK_GS26S2_DENUNCIA_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

-- "Conversa do bairro". DT_EDICAO só é preenchida quando o autor edita o comentário.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_COMENTARIO
       (    CD_COMENTARIO NUMBER(10,0),
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        CD_USUARIO NUMBER(10,0) NOT NULL,
        DS_COMENTARIO VARCHAR2(500) NOT NULL,
        DT_CRIACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        DT_EDICAO TIMESTAMP (6),
         CONSTRAINT PK_GS26S2_COMENTARIO PRIMARY KEY (CD_COMENTARIO),
         CONSTRAINT FK_GS26S2_COMENTARIO_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_GS26S2_DENUNCIA (CD_DENUNCIA),
         CONSTRAINT FK_GS26S2_COMENTARIO_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

-- Voto (UP/DOWN). A chave composta garante um voto por pessoa em cada denúncia.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_VOTO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_VOTO_DENUNCIA
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        TP_VOTO VARCHAR2(4) NOT NULL,
        DT_VOTO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT CK_GS26S2_VOTO_DENUNCIA_TIPO CHECK (tp_voto IN ('UP','DOWN')),
         CONSTRAINT PK_GS26S2_VOTO_DENUNCIA PRIMARY KEY (CD_USUARIO, CD_DENUNCIA),
         CONSTRAINT FK_GS26S2_VOTO_DENUNCIA_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO),
         CONSTRAINT FK_GS26S2_VOTO_DENUNCIA_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_GS26S2_DENUNCIA (CD_DENUNCIA)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_VOTO_DENUNCIA');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_VOTO_DENUNCIA já existe: mantida como está');
  END IF;
END;
/

-- "Já foi resolvido": na 3ª confirmação de moradores diferentes, a denúncia vira RESOLVIDA.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_CONFIRMACAO_RESOLUCAO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_CONFIRMACAO_RESOLUCAO
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_DENUNCIA NUMBER(10,0) NOT NULL,
        DT_CONFIRMACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT PK_GS26S2_CONFIRMACAO_RESOLUCAO PRIMARY KEY (CD_USUARIO, CD_DENUNCIA),
         CONSTRAINT FK_GS26S2_CONFIRMACAO_RESOLUCAO_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO),
         CONSTRAINT FK_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA FOREIGN KEY (CD_DENUNCIA)
          REFERENCES TB_GS26S2_DENUNCIA (CD_DENUNCIA)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_CONFIRMACAO_RESOLUCAO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_CONFIRMACAO_RESOLUCAO já existe: mantida como está');
  END IF;
END;
/

-- Reporte de comentário ofensivo. A chave composta garante um reporte por pessoa em cada comentário;
-- com 3 reportes o comentário deixa de aparecer na API (ComentarioService.REPORTES_PARA_OCULTAR).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_REPORTE_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_REPORTE_COMENTARIO
       (    CD_USUARIO NUMBER(10,0) NOT NULL,
        CD_COMENTARIO NUMBER(10,0) NOT NULL,
        DT_REPORTE TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT PK_GS26S2_REPORTE_COMENTARIO PRIMARY KEY (CD_USUARIO, CD_COMENTARIO),
         CONSTRAINT FK_GS26S2_REPORTE_COMENTARIO_USUARIO FOREIGN KEY (CD_USUARIO)
          REFERENCES TB_GS26S2_USUARIO (CD_USUARIO),
         CONSTRAINT FK_GS26S2_REPORTE_COMENTARIO_COMENTARIO FOREIGN KEY (CD_COMENTARIO)
          REFERENCES TB_GS26S2_COMENTARIO (CD_COMENTARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_REPORTE_COMENTARIO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_REPORTE_COMENTARIO já existe: mantida como está');
  END IF;
END;
/

-- Catálogo de temas de direitos (tela "Meus Direitos").
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_DIREITO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_DIREITO
       (    CD_DIREITO NUMBER(10,0),
        DS_TITULO VARCHAR2(120) NOT NULL,
        DS_RESUMO VARCHAR2(200) NOT NULL,
        DS_DIREITO VARCHAR2(1000) NOT NULL,
        DS_AVISO VARCHAR2(300),
        DS_BASE_LEGAL VARCHAR2(300),
        DS_LINK_OFICIAL VARCHAR2(300),
        TP_ICONE VARCHAR2(15) DEFAULT 'OUTRO' NOT NULL,
        NR_ORDEM NUMBER(3,0) DEFAULT 0 NOT NULL,
         CONSTRAINT CK_GS26S2_DIREITO_ICONE CHECK (tp_icone IN
                         ('TRANSPORTE','PRIORIDADE','MUNICIPIO','EDUCACAO','SAUDE','RENDA','OUTRO')),
         CONSTRAINT PK_GS26S2_DIREITO PRIMARY KEY (CD_DIREITO),
         CONSTRAINT UK_GS26S2_DIREITO_TITULO UNIQUE (DS_TITULO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_DIREITO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_DIREITO já existe: mantida como está');
  END IF;
END;
/

-- Passos "Por onde começar" de cada tema (1 a 10).
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_DIREITO_PASSO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_DIREITO_PASSO
       (    CD_DIREITO NUMBER(10,0) NOT NULL,
        NR_ORDEM NUMBER(2,0) NOT NULL,
        DS_TITULO VARCHAR2(80) NOT NULL,
        DS_PASSO VARCHAR2(400) NOT NULL,
         CONSTRAINT CK_GS26S2_DIREITO_PASSO_ORDEM CHECK (nr_ordem BETWEEN 1 AND 10),
         CONSTRAINT PK_GS26S2_DIREITO_PASSO PRIMARY KEY (CD_DIREITO, NR_ORDEM),
         CONSTRAINT FK_GS26S2_DIREITO_PASSO_DIREITO FOREIGN KEY (CD_DIREITO)
          REFERENCES TB_GS26S2_DIREITO (CD_DIREITO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_DIREITO_PASSO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_DIREITO_PASSO já existe: mantida como está');
  END IF;
END;
/

-- Públicos a que o tema se aplica. TODOS = qualquer pessoa que respondeu o questionário.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_DIREITO_TAG';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_DIREITO_TAG
       (    CD_DIREITO NUMBER(10,0) NOT NULL,
        TP_TAG VARCHAR2(20) NOT NULL,
         CONSTRAINT CK_GS26S2_DIREITO_TAG_TAG CHECK (tp_tag IN
                    ('IDOSO','JOVEM','PCD','PCD_MOBILIDADE','PPI','ESCOLA_PUBLICA','BAIXA_RENDA','TODOS')),
         CONSTRAINT PK_GS26S2_DIREITO_TAG PRIMARY KEY (CD_DIREITO, TP_TAG),
         CONSTRAINT FK_GS26S2_DIREITO_TAG_DIREITO FOREIGN KEY (CD_DIREITO)
          REFERENCES TB_GS26S2_DIREITO (CD_DIREITO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_DIREITO_TAG');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_DIREITO_TAG já existe: mantida como está');
  END IF;
END;
/

-- "Cidade viva": cidades que já receberam o conteúdo de demonstração e o último pulso de atividade.
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_SEMEADURA_CIDADE';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_SEMEADURA_CIDADE
       (    NM_CIDADE_CHAVE VARCHAR2(80) NOT NULL,
        SG_UF CHAR(2) NOT NULL,
        NM_CIDADE VARCHAR2(80) NOT NULL,
        DT_SEMEADURA TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        DT_ULTIMO_PULSO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
        QT_EVENTOS NUMBER(6,0) DEFAULT 0 NOT NULL,
         CONSTRAINT PK_GS26S2_SEMEADURA_CIDADE PRIMARY KEY (NM_CIDADE_CHAVE, SG_UF)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_SEMEADURA_CIDADE');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_SEMEADURA_CIDADE já existe: mantida como está');
  END IF;
END;
/

-- =====================================================================================
-- 3. Índices (as chaves primárias e únicas já criam os seus)
-- =====================================================================================

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_COMENTARIO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_COMENTARIO_DENUNCIA ON TB_GS26S2_COMENTARIO (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_COMENTARIO_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_COMENTARIO_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_COMENTARIO_USUARIO ON TB_GS26S2_COMENTARIO (CD_USUARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_COMENTARIO_USUARIO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA ON TB_GS26S2_CONFIRMACAO_RESOLUCAO (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_DENUNCIA_CIDADE';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_DENUNCIA_CIDADE ON TB_GS26S2_DENUNCIA (NM_CIDADE, SG_UF)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_DENUNCIA_CIDADE');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_DENUNCIA_USUARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_DENUNCIA_USUARIO ON TB_GS26S2_DENUNCIA (CD_USUARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_DENUNCIA_USUARIO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_DIREITO_TAG_TAG';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_DIREITO_TAG_TAG ON TB_GS26S2_DIREITO_TAG (TP_TAG)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_DIREITO_TAG_TAG');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_USUARIO_SIMULADO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_USUARIO_SIMULADO ON TB_GS26S2_USUARIO (ST_SIMULADO, SG_UF)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_USUARIO_SIMULADO');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_VOTO_DENUNCIA_DENUNCIA';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_VOTO_DENUNCIA_DENUNCIA ON TB_GS26S2_VOTO_DENUNCIA (CD_DENUNCIA)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_VOTO_DENUNCIA_DENUNCIA');
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_REPORTE_COMENTARIO_COMENTARIO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_REPORTE_COMENTARIO_COMENTARIO ON TB_GS26S2_REPORTE_COMENTARIO (CD_COMENTARIO)]';
    DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_REPORTE_COMENTARIO_COMENTARIO');
  END IF;
END;
/

-- Índices baseados em função para a comparação de cidade (feed, busca, relatórios, "cidade viva").
-- A API compara a cidade sem acentos nem maiúsculas com a expressão gerada por
-- JdbcUtil.cidadeIgual(coluna) no back-end (os espaços repetidos já são removidos ao gravar, porque
-- REGEXP_REPLACE não pode ser indexado: ORA-01743):
--   TRANSLATE(UPPER(TRIM(nm_cidade)), '<com acento>', '<sem acento>') = ?
-- Um índice comum em NM_CIDADE não serve para essa comparação. O Oracle só usa o índice abaixo se a
-- expressão da consulta for IDÊNTICA à do índice (mesmas funções, mesma ordem, mesmas listas de
-- caracteres); se JdbcUtil.cidadeIgual mudar, este índice precisa ser recriado com a nova expressão
-- (o teste IndiceCidadeScriptTest do back-end confere que as duas continuam iguais).
-- Se o Oracle recusar o índice (ex.: ORA-01743, "só funções puras podem ser indexadas"), o bloco apenas
-- registra a falha com DBMS_OUTPUT e o script continua: a API funciona igual, só sem o índice.

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_DENUNCIA_CIDADE_CHAVE';
  IF existe = 0 THEN
    BEGIN
      EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_DENUNCIA_CIDADE_CHAVE ON TB_GS26S2_DENUNCIA
        (TRANSLATE(UPPER(TRIM(nm_cidade)), 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'AAAAAEEEEIIIIOOOOOUUUUCN'), sg_uf)]';
      DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_DENUNCIA_CIDADE_CHAVE');
    EXCEPTION
      WHEN OTHERS THEN
        -- o índice é só otimização: se o Oracle recusar (ex.: ORA-01743), registra e segue o script
        DBMS_OUTPUT.PUT_LINE('Índice IX_GS26S2_DENUNCIA_CIDADE_CHAVE não criado (a API funciona sem ele): ' || SQLERRM);
    END;
  END IF;
END;
/

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_indexes WHERE index_name = 'IX_GS26S2_USUARIO_CIDADE_CHAVE';
  IF existe = 0 THEN
    BEGIN
      EXECUTE IMMEDIATE q'[CREATE INDEX IX_GS26S2_USUARIO_CIDADE_CHAVE ON TB_GS26S2_USUARIO
        (TRANSLATE(UPPER(TRIM(nm_cidade)), 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ', 'AAAAAEEEEIIIIOOOOOUUUUCN'), sg_uf)]';
      DBMS_OUTPUT.PUT_LINE('Criado o índice IX_GS26S2_USUARIO_CIDADE_CHAVE');
    EXCEPTION
      WHEN OTHERS THEN
        -- o índice é só otimização: se o Oracle recusar (ex.: ORA-01743), registra e segue o script
        DBMS_OUTPUT.PUT_LINE('Índice IX_GS26S2_USUARIO_CIDADE_CHAVE não criado (a API funciona sem ele): ' || SQLERRM);
    END;
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
  SELECT COUNT(*) INTO existe FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(q'[Transporte e mobilidade]');
  IF existe = 0 THEN
    INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_gs26s2_direito.NEXTVAL, q'[Transporte e mobilidade]',
              q'[Saiba onde consultar gratuidades e programas de transporte.]',
              q'[Passe Livre e outros programas de transporte. Exemplos para pesquisa.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              q'[Lei 10.741/2003 (Estatuto da Pessoa Idosa), arts. 39 e 40; Lei 8.899/1994 (Passe Livre interestadual)]',
              q'[https://www.gov.br/pt-br/servicos/obter-o-passe-livre-interestadual]', 'TRANSPORTE', 1)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Identifique o programa]', q'[Verifique se o deslocamento é municipal, intermunicipal ou interestadual.]');
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Consulte o órgão responsável]', q'[Procure o portal oficial do serviço de transporte e confira os critérios atuais.]');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD_MOBILIDADE');
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
  SELECT COUNT(*) INTO existe FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(q'[Atendimento prioritário]');
  IF existe = 0 THEN
    INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_gs26s2_direito.NEXTVAL, q'[Atendimento prioritário]',
              q'[Entenda como consultar as regras de atendimento.]',
              q'[Informação para ajudar você a solicitar atendimento.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              q'[Lei 10.048/2000 (atendimento prioritário)]',
              q'[https://www.planalto.gov.br/ccivil_03/leis/l10048.htm]', 'PRIORIDADE', 2)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Entenda as regras]', q'[Consulte a legislação aplicável e as orientações oficiais do serviço.]');
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Verifique a documentação]', q'[Confira com o estabelecimento ou órgão quais comprovantes são necessários.]');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
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
  SELECT COUNT(*) INTO existe FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(q'[Benefícios municipais]');
  IF existe = 0 THEN
    INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_gs26s2_direito.NEXTVAL, q'[Benefícios municipais]',
              q'[Consulte possíveis isenções e apoios na sua cidade.]',
              q'[Isenção de IPTU para idosos e outros apoios podem depender de regras locais.]',
              q'[Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.]',
              NULL,
              NULL, 'MUNICIPIO', 3)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Consulte sua prefeitura]', q'[Busque o tema no portal oficial da cidade, sem presumir que o benefício existe.]');
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Confira os critérios]', q'[Idade, renda, imóvel e outros requisitos precisam ser verificados no município.]');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'TODOS');
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
  SELECT COUNT(*) INTO existe FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(q'[Educação e cotas]');
  IF existe = 0 THEN
    INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_gs26s2_direito.NEXTVAL, q'[Educação e cotas]',
              q'[Veja como funcionam as cotas no ensino superior público.]',
              q'[Universidades e institutos federais reservam vagas para quem estudou em escola pública, com subcotas para pessoas pretas, pardas, indígenas e com deficiência.]',
              q'[Cada processo seletivo publica suas próprias regras no edital. Leia o edital antes de se inscrever.]',
              q'[Lei 12.711/2012 (Lei de Cotas)]',
              q'[https://www.gov.br/mec/pt-br]', 'EDUCACAO', 4)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Confira seu percurso escolar]', q'[As cotas exigem, em geral, o ensino médio completo em escola pública.]');
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Leia o edital]', q'[No Sisu ou no vestibular, verifique as modalidades de cota e os documentos exigidos.]');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'ESCOLA_PUBLICA');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PPI');
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
  SELECT COUNT(*) INTO existe FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(q'[Assistência e renda]');
  IF existe = 0 THEN
    INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
      VALUES (seq_gs26s2_direito.NEXTVAL, q'[Assistência e renda]',
              q'[Programas sociais que podem apoiar famílias de baixa renda.]',
              q'[Benefícios como o BPC e a Tarifa Social de Energia dependem do Cadastro Único e de critérios de renda.]',
              q'[A aprovação depende de análise do órgão responsável. Procure o CRAS da sua região.]',
              q'[Lei 8.742/1993 (LOAS); Lei 12.212/2010 (Tarifa Social de Energia)]',
              q'[https://www.gov.br/mds/pt-br]', 'RENDA', 5)
      RETURNING cd_direito INTO id_tema;
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 1, q'[Atualize o Cadastro Único]', q'[Procure o CRAS com documentos de todos da família para fazer ou atualizar o cadastro.]');
    INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo)
      VALUES (id_tema, 2, q'[Pergunte sobre os programas]', q'[No atendimento, peça orientação sobre os benefícios para os quais sua família pode ser avaliada.]');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'BAIXA_RENDA');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'IDOSO');
    INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (id_tema, 'PCD');
    DBMS_OUTPUT.PUT_LINE(q'[Inserido o tema Assistência e renda]');
  ELSE
    DBMS_OUTPUT.PUT_LINE(q'[Tema Assistência e renda já existe: mantido como está]');
  END IF;
END;
/

COMMIT;
