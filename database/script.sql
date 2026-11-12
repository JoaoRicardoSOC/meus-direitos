-- =====================================================================================
-- Meus Direitos · FIAP Global Solution 2026/2 (CitizenTech)
-- Script do banco de dados (Oracle): estrutura (DDL) e carga inicial.
--
-- Como executar: no SQL Developer, abra este arquivo e use "Executar Script" (F5),
-- conectado ao schema que a API usa (db.usuario do backend/db.properties).
-- O script pode ser executado de novo: ele apaga e recria tudo (os dados são perdidos).
--
-- Convenções (as mesmas do back-end, em dao.jdbc.JdbcUtil):
--   * prefixos: tb_ tabela, cd_ código, ds_ descrição, nm_ nome, sg_ sigla, dt_ data,
--     tp_ tipo (enum), st_ situação/flag, nr_ número, qt_ quantidade;
--   * booleanos em CHAR(1) 'S'/'N';
--   * enums gravados pelo nome da constante Java, com CHECK aceitando os mesmos valores;
--   * textos em VARCHAR2(n CHAR): o limite é em caracteres, como na validação da API
--     (com BYTE, um texto acentuado no limite estouraria a coluna);
--   * sem ON DELETE CASCADE: a API apaga as tabelas filhas antes, na mesma transação.
-- =====================================================================================


-- =====================================================================================
-- 1. Limpeza (ignora objetos que ainda não existem)
-- =====================================================================================

BEGIN
    FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN (
                  'TB_CONFIRMACAO_RESOLUCAO', 'TB_VOTO_DENUNCIA', 'TB_COMENTARIO', 'TB_DENUNCIA',
                  'TB_PERFIL_DEMOGRAFICO', 'TB_DIREITO_TAG', 'TB_DIREITO_PASSO', 'TB_DIREITO',
                  'TB_SEMEADURA_CIDADE', 'TB_USUARIO')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name IN (
                  'SEQ_USUARIO', 'SEQ_DENUNCIA', 'SEQ_COMENTARIO', 'SEQ_DIREITO')) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/


-- =====================================================================================
-- 2. Sequences (chaves geradas pela API com seq_xxx.NEXTVAL)
-- =====================================================================================

CREATE SEQUENCE seq_usuario    START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_denuncia   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_comentario START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_direito    START WITH 1 INCREMENT BY 1 NOCACHE;


-- =====================================================================================
-- 3. Tabelas
-- =====================================================================================

-- Conta do usuário. st_simulado = 'S' marca os moradores de exemplo da "cidade viva".
CREATE TABLE tb_usuario (
    cd_usuario     NUMBER(10)          NOT NULL,
    nm_usuario     VARCHAR2(100 CHAR)  NOT NULL,
    ds_email       VARCHAR2(120 CHAR)  NOT NULL,
    ds_senha_hash  VARCHAR2(200 CHAR)  NOT NULL,  -- PBKDF2 "iteracoes:salt:hash" (nunca a senha)
    nm_cidade      VARCHAR2(80 CHAR)   NOT NULL,
    sg_uf          CHAR(2)             NOT NULL,
    dt_cadastro    DATE                NOT NULL,
    st_simulado    CHAR(1)             DEFAULT 'N' NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (cd_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (ds_email),
    CONSTRAINT ck_usuario_uf CHECK (sg_uf IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
        'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO')),
    CONSTRAINT ck_usuario_simulado CHECK (st_simulado IN ('S', 'N'))
);

-- Questionário "Meus Direitos" (opcional, 0..1 por usuário). Todas as respostas podem ficar em branco.
CREATE TABLE tb_perfil_demografico (
    cd_usuario           NUMBER(10)         NOT NULL,
    dt_nascimento        DATE,
    tp_raca_cor          VARCHAR2(20 CHAR),
    tp_deficiencia       VARCHAR2(20 CHAR),
    tp_percurso_escolar  VARCHAR2(20 CHAR),
    st_baixa_renda       CHAR(1),
    dt_atualizacao       DATE               NOT NULL,
    CONSTRAINT pk_perfil_demografico PRIMARY KEY (cd_usuario),
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT ck_perfil_raca_cor CHECK (tp_raca_cor IN (
        'BRANCA', 'PRETA', 'PARDA', 'AMARELA', 'INDIGENA', 'NAO_INFORMADO')),
    CONSTRAINT ck_perfil_deficiencia CHECK (tp_deficiencia IN (
        'NENHUMA', 'FISICA', 'VISUAL', 'AUDITIVA', 'INTELECTUAL', 'NAO_INFORMADO')),
    CONSTRAINT ck_perfil_percurso CHECK (tp_percurso_escolar IN (
        'ESCOLA_PUBLICA', 'ESCOLA_PARTICULAR', 'MISTO', 'NAO_INFORMADO')),
    CONSTRAINT ck_perfil_baixa_renda CHECK (st_baixa_renda IN ('S', 'N'))
);

-- Problema urbano registrado por um morador, na cidade do cadastro dele.
CREATE TABLE tb_denuncia (
    cd_denuncia     NUMBER(10)           NOT NULL,
    cd_usuario      NUMBER(10)           NOT NULL,
    ds_titulo       VARCHAR2(120 CHAR)   NOT NULL,
    ds_denuncia     VARCHAR2(1000 CHAR)  NOT NULL,
    tp_categoria    VARCHAR2(30 CHAR)    NOT NULL,
    st_denuncia     VARCHAR2(20 CHAR)    DEFAULT 'ABERTA' NOT NULL,
    ds_endereco     VARCHAR2(150 CHAR)   NOT NULL,
    nm_bairro       VARCHAR2(80 CHAR),
    nm_cidade       VARCHAR2(80 CHAR)    NOT NULL,
    sg_uf           CHAR(2)              NOT NULL,
    ds_imagem_url   VARCHAR2(300 CHAR),
    dt_criacao      DATE                 NOT NULL,
    dt_atualizacao  DATE                 NOT NULL,
    CONSTRAINT pk_denuncia PRIMARY KEY (cd_denuncia),
    CONSTRAINT fk_denuncia_usuario FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT ck_denuncia_categoria CHECK (tp_categoria IN (
        'ASFALTO', 'ILUMINACAO', 'CALCADA_ACESSIBILIDADE', 'SEGURANCA', 'LIMPEZA_URBANA', 'OUTROS')),
    CONSTRAINT ck_denuncia_status CHECK (st_denuncia IN ('ABERTA', 'EM_ANDAMENTO', 'RESOLVIDA')),
    CONSTRAINT ck_denuncia_uf CHECK (sg_uf IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
        'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'))
);

-- "Conversa do bairro". dt_edicao só é preenchida quando o autor edita o comentário.
CREATE TABLE tb_comentario (
    cd_comentario  NUMBER(10)          NOT NULL,
    cd_denuncia    NUMBER(10)          NOT NULL,
    cd_usuario     NUMBER(10)          NOT NULL,
    ds_comentario  VARCHAR2(500 CHAR)  NOT NULL,
    dt_criacao     DATE                NOT NULL,
    dt_edicao      DATE,
    CONSTRAINT pk_comentario PRIMARY KEY (cd_comentario),
    CONSTRAINT fk_comentario_denuncia FOREIGN KEY (cd_denuncia) REFERENCES tb_denuncia (cd_denuncia),
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario)
);

-- Voto (↑/↓). A chave composta garante um voto por pessoa em cada denúncia.
CREATE TABLE tb_voto_denuncia (
    cd_usuario   NUMBER(10)        NOT NULL,
    cd_denuncia  NUMBER(10)        NOT NULL,
    tp_voto      VARCHAR2(4 CHAR)  NOT NULL,
    dt_voto      DATE              NOT NULL,
    CONSTRAINT pk_voto_denuncia PRIMARY KEY (cd_usuario, cd_denuncia),
    CONSTRAINT fk_voto_usuario FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_voto_denuncia FOREIGN KEY (cd_denuncia) REFERENCES tb_denuncia (cd_denuncia),
    CONSTRAINT ck_voto_tipo CHECK (tp_voto IN ('UP', 'DOWN'))
);

-- "Já foi resolvido": na 3ª confirmação de moradores diferentes, a denúncia vira RESOLVIDA.
CREATE TABLE tb_confirmacao_resolucao (
    cd_usuario      NUMBER(10)  NOT NULL,
    cd_denuncia     NUMBER(10)  NOT NULL,
    dt_confirmacao  DATE        NOT NULL,
    CONSTRAINT pk_confirmacao_resolucao PRIMARY KEY (cd_usuario, cd_denuncia),
    CONSTRAINT fk_confirmacao_usuario FOREIGN KEY (cd_usuario) REFERENCES tb_usuario (cd_usuario),
    CONSTRAINT fk_confirmacao_denuncia FOREIGN KEY (cd_denuncia) REFERENCES tb_denuncia (cd_denuncia)
);

-- Catálogo de temas de direitos (tela "Meus Direitos").
CREATE TABLE tb_direito (
    cd_direito       NUMBER(10)           NOT NULL,
    ds_titulo        VARCHAR2(120 CHAR)   NOT NULL,
    ds_resumo        VARCHAR2(200 CHAR)   NOT NULL,
    ds_direito       VARCHAR2(1000 CHAR)  NOT NULL,
    ds_aviso         VARCHAR2(300 CHAR),
    ds_base_legal    VARCHAR2(300 CHAR),
    ds_link_oficial  VARCHAR2(300 CHAR),
    tp_icone         VARCHAR2(20 CHAR)    NOT NULL,
    nr_ordem         NUMBER(3)            DEFAULT 0 NOT NULL,
    CONSTRAINT pk_direito PRIMARY KEY (cd_direito),
    CONSTRAINT ck_direito_icone CHECK (tp_icone IN (
        'TRANSPORTE', 'PRIORIDADE', 'MUNICIPIO', 'EDUCACAO', 'SAUDE', 'RENDA', 'OUTRO')),
    CONSTRAINT ck_direito_ordem CHECK (nr_ordem BETWEEN 0 AND 999)
);

-- Passos "Por onde começar" de cada tema (1 a 10, numerados a partir de 1).
CREATE TABLE tb_direito_passo (
    cd_direito  NUMBER(10)          NOT NULL,
    nr_ordem    NUMBER(2)           NOT NULL,
    ds_titulo   VARCHAR2(80 CHAR)   NOT NULL,
    ds_passo    VARCHAR2(400 CHAR)  NOT NULL,
    CONSTRAINT pk_direito_passo PRIMARY KEY (cd_direito, nr_ordem),
    CONSTRAINT fk_passo_direito FOREIGN KEY (cd_direito) REFERENCES tb_direito (cd_direito)
);

-- Públicos a que o tema se aplica. TODOS = aparece para qualquer pessoa que respondeu o questionário.
CREATE TABLE tb_direito_tag (
    cd_direito  NUMBER(10)         NOT NULL,
    tp_tag      VARCHAR2(20 CHAR)  NOT NULL,
    CONSTRAINT pk_direito_tag PRIMARY KEY (cd_direito, tp_tag),
    CONSTRAINT fk_tag_direito FOREIGN KEY (cd_direito) REFERENCES tb_direito (cd_direito),
    CONSTRAINT ck_direito_tag CHECK (tp_tag IN (
        'IDOSO', 'JOVEM', 'PCD', 'PCD_MOBILIDADE', 'PPI', 'ESCOLA_PUBLICA', 'BAIXA_RENDA', 'TODOS'))
);

-- "Cidade viva": cidades que já receberam o conteúdo de demonstração e o último pulso de atividade.
-- nm_cidade_chave é o nome em maiúsculas e sem acento ("SAO PAULO"); a chave primária impede
-- que duas requisições simultâneas semeiem a mesma cidade.
CREATE TABLE tb_semeadura_cidade (
    nm_cidade_chave  VARCHAR2(80 CHAR)  NOT NULL,
    sg_uf            CHAR(2)            NOT NULL,
    nm_cidade        VARCHAR2(80 CHAR)  NOT NULL,
    dt_semeadura     DATE               NOT NULL,
    dt_ultimo_pulso  DATE               NOT NULL,
    qt_eventos       NUMBER(10)         DEFAULT 0 NOT NULL,
    CONSTRAINT pk_semeadura_cidade PRIMARY KEY (nm_cidade_chave, sg_uf)
);


-- =====================================================================================
-- 4. Índices
-- =====================================================================================

-- Chaves estrangeiras (exclusões em lote e contagens por denúncia/usuário)
CREATE INDEX ix_denuncia_usuario     ON tb_denuncia (cd_usuario);
CREATE INDEX ix_comentario_denuncia  ON tb_comentario (cd_denuncia);
CREATE INDEX ix_comentario_usuario   ON tb_comentario (cd_usuario);
CREATE INDEX ix_voto_denuncia        ON tb_voto_denuncia (cd_denuncia);
CREATE INDEX ix_confirmacao_denuncia ON tb_confirmacao_resolucao (cd_denuncia);

-- Feed, busca e relatórios filtram pela UF (a cidade é comparada sem acento, por função)
CREATE INDEX ix_denuncia_uf_criacao  ON tb_denuncia (sg_uf, dt_criacao);
CREATE INDEX ix_usuario_uf           ON tb_usuario (sg_uf);

-- Título de tema único, sem diferenciar maiúsculas (a API responde 409 ao repetir)
CREATE UNIQUE INDEX uk_direito_titulo ON tb_direito (UPPER(ds_titulo));
