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


-- =====================================================================================
-- 5. Carga inicial: catálogo de direitos
-- =====================================================================================
-- Linguagem cautelosa: cada tema indica uma possibilidade a verificar e o caminho oficial.
-- O tema aparece em GET /direitos/meus quando uma das suas tags bate com o perfil da pessoa
-- (TODOS aparece para qualquer perfil respondido). Revise as regras antes de cada entrega:
-- valores e critérios de programas sociais mudam com frequência.

SET DEFINE OFF;

-- 1. Transporte e mobilidade
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Transporte e mobilidade',
        'Saiba onde consultar gratuidades e programas de transporte.',
        'Passe Livre e outros programas de transporte. Pessoas a partir de 65 anos podem ter gratuidade no transporte coletivo urbano, e muitas cidades estendem o benefício a partir dos 60. Pessoas com deficiência de baixa renda podem ter o Passe Livre interestadual. Exemplos para pesquisa.',
        'Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.',
        'Lei 10.741/2003 (Estatuto da Pessoa Idosa), arts. 39 e 40; Lei 8.899/1994',
        'https://www.gov.br/pt-br/servicos/obter-o-passe-livre-interestadual', 'TRANSPORTE', 1);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Identifique o programa',
        'Verifique se o deslocamento é municipal, intermunicipal ou interestadual.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Consulte o órgão responsável',
        'Procure o portal oficial do serviço de transporte e confira os critérios atuais.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 3, 'Separe os documentos',
        'Em geral pedem documento com foto e, conforme o programa, comprovante de residência, laudo médico ou inscrição no CadÚnico.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'IDOSO');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PCD');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PCD_MOBILIDADE');

-- 2. Atendimento prioritário
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Atendimento prioritário',
        'Filas e atendimento preferencial em órgãos públicos, bancos e serviços.',
        'Pessoas idosas (60+) e pessoas com deficiência podem ter atendimento preferencial em repartições públicas, bancos, serviços de saúde e no comércio. A partir dos 80 anos, a prioridade pode ser ainda maior, à frente das demais pessoas idosas.',
        'Cada local organiza a prioridade de um jeito. Se ela não for respeitada, procure a ouvidoria do serviço.',
        'Lei 10.048/2000; Lei 10.741/2003 (Estatuto da Pessoa Idosa), art. 3º; Lei 13.146/2015 (Estatuto da Pessoa com Deficiência), art. 9º',
        'https://www.planalto.gov.br/ccivil_03/leis/l10048.htm', 'PRIORIDADE', 2);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Leve um documento',
        'Tenha em mãos um documento com foto que comprove a idade ou, se for o caso, um documento que indique a deficiência.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Procure a fila ou o guichê preferencial',
        'Se não houver sinalização, peça atendimento preferencial a um funcionário.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 3, 'Registre a reclamação',
        'Se o direito não for respeitado, registre na ouvidoria do órgão ou do banco, ou no Procon da sua cidade.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'IDOSO');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PCD');

-- 3. BPC
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Benefício de Prestação Continuada (BPC)',
        'Um salário mínimo por mês para pessoa idosa ou com deficiência de família de baixa renda.',
        'O BPC pode ser concedido a pessoas a partir de 65 anos ou a pessoas com deficiência de qualquer idade, quando a renda da família é baixa. Não é aposentadoria e não exige ter contribuído com o INSS. A inscrição no CadÚnico é obrigatória.',
        'Os limites de renda e as regras de avaliação mudam. Confirme os critérios atuais no CRAS ou no INSS.',
        'Lei 8.742/1993 (Lei Orgânica da Assistência Social), art. 20',
        'https://www.planalto.gov.br/ccivil_03/leis/l8742.htm', 'RENDA', 3);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Atualize o CadÚnico',
        'Procure o CRAS da sua região para fazer ou atualizar a inscrição da família no Cadastro Único.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Faça o pedido no INSS',
        'O pedido pode ser feito pelo aplicativo ou site Meu INSS, ou pela central 135.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 3, 'Acompanhe as avaliações',
        'Para pessoas com deficiência há avaliação médica e social. Acompanhe o andamento e as convocações pelo Meu INSS.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'BAIXA_RENDA');

-- 4. Tarifa Social de Energia Elétrica
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Tarifa Social de Energia Elétrica',
        'Desconto na conta de luz para famílias inscritas no CadÚnico.',
        'Famílias de baixa renda inscritas no CadÚnico, ou com alguém que recebe o BPC, podem ter desconto na conta de luz. Em muitos casos o desconto é aplicado automaticamente quando os dados do CadÚnico estão atualizados.',
        'As faixas de desconto e os critérios mudam. Confirme com a distribuidora de energia da sua cidade.',
        'Lei 12.212/2010',
        'https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2010/lei/l12212.htm', 'RENDA', 4);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Confira o CadÚnico',
        'Verifique no CRAS se a inscrição da família está ativa e atualizada.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Fale com a distribuidora',
        'Informe o NIS e o número da instalação à distribuidora de energia e pergunte se o desconto já está aplicado.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'BAIXA_RENDA');

-- 5. ID Jovem
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Identidade Jovem (ID Jovem)',
        'Viagens interestaduais gratuitas ou com desconto e meia-entrada para jovens de baixa renda.',
        'Jovens de 15 a 29 anos de famílias inscritas no CadÚnico podem emitir a ID Jovem, que dá acesso a vagas gratuitas ou com desconto no transporte interestadual e à meia-entrada em eventos culturais e esportivos.',
        'É preciso ter de 15 a 29 anos e CadÚnico atualizado. Confirme as regras no canal oficial da ID Jovem.',
        'Lei 12.852/2013 (Estatuto da Juventude), arts. 23 e 32; Decreto 8.537/2015',
        'https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2015/decreto/d8537.htm', 'TRANSPORTE', 5);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Tenha o NIS em mãos',
        'O NIS é o número da inscrição no CadÚnico. Se não souber, pergunte no CRAS.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Emita a carteira',
        'Emita a ID Jovem pelo aplicativo oficial ou pelo portal gov.br e guarde a versão digital.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 3, 'Reserve com antecedência',
        'As vagas gratuitas no transporte interestadual são limitadas: procure a empresa de ônibus antes da viagem.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'BAIXA_RENDA');

-- 6. Cotas no ensino superior
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Cotas no ensino superior',
        'Reserva de vagas em universidades e institutos federais.',
        'Metade das vagas das universidades e institutos federais é reservada a quem cursou todo o ensino médio em escola pública. Dentro dessa reserva há vagas para pessoas pretas, pardas e indígenas, quilombolas, pessoas com deficiência e estudantes de baixa renda.',
        'Cada edital (Sisu, vestibular próprio) detalha os documentos e as comissões de verificação. Leia o edital da instituição.',
        'Lei 12.711/2012 (Lei de Cotas), atualizada pela Lei 14.723/2023',
        'https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2012/lei/l12711.htm', 'EDUCACAO', 6);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Confira se você se enquadra',
        'Veja no edital qual modalidade de cota corresponde ao seu percurso escolar, renda, raça/cor ou deficiência.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Reúna os documentos',
        'Histórico escolar do ensino médio, comprovantes de renda da família e, se for o caso, autodeclaração ou laudo.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 3, 'Escolha a modalidade na inscrição',
        'No Sisu ou no vestibular, marque a modalidade de cota correta e acompanhe os prazos de matrícula.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'ESCOLA_PUBLICA');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PPI');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PCD');

-- 7. Meia-entrada
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Meia-entrada em cultura e esporte',
        'Pague metade do ingresso em cinemas, shows, teatros e eventos esportivos.',
        'Têm direito à meia-entrada: pessoas a partir de 60 anos, estudantes, pessoas com deficiência (e o acompanhante, quando necessário) e jovens de 15 a 29 anos de baixa renda com a ID Jovem.',
        'Os eventos podem limitar a quantidade de ingressos com meia-entrada. Leve o documento que comprova o direito.',
        'Lei 12.933/2013; Lei 10.741/2003 (Estatuto da Pessoa Idosa), art. 23',
        'https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2013/lei/l12933.htm', 'OUTRO', 7);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Separe o comprovante',
        'Documento com foto (idade), carteira estudantil, ID Jovem ou documento que indique a deficiência.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Escolha meia-entrada na compra',
        'Na bilheteria ou no site, selecione o ingresso de meia-entrada e apresente o comprovante na entrada.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'IDOSO');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'PCD');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'JOVEM');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'BAIXA_RENDA');

-- 8. Saúde
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Saúde pública (SUS)',
        'Atendimento gratuito, vacinas e medicamentos pelo SUS.',
        'Qualquer pessoa pode ser atendida gratuitamente pelo SUS: consultas na Unidade Básica de Saúde (UBS), vacinas, exames, urgência e emergência. Alguns medicamentos são gratuitos ou têm desconto pelo programa Farmácia Popular.',
        'A oferta de serviços varia por município. Procure a UBS mais próxima para saber o que está disponível.',
        'Constituição Federal, art. 196; Lei 8.080/1990',
        'https://www.planalto.gov.br/ccivil_03/leis/l8080.htm', 'SAUDE', 8);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Tenha o Cartão SUS',
        'Faça o Cartão Nacional de Saúde na UBS ou consulte o número no aplicativo Meu SUS Digital.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Procure a UBS da sua região',
        'A UBS é a porta de entrada: ela agenda consultas e encaminha para especialistas e exames.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'TODOS');

-- 9. Benefícios municipais
INSERT INTO tb_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, ds_link_oficial, tp_icone, nr_ordem)
VALUES (seq_direito.NEXTVAL, 'Benefícios da sua cidade',
        'Programas próprios da prefeitura: isenções, auxílios e serviços locais.',
        'Muitas prefeituras têm programas próprios, como isenção de IPTU para pessoas idosas ou de baixa renda, auxílios emergenciais, cursos gratuitos e transporte especial para pessoas com deficiência. Os programas mudam de cidade para cidade.',
        'Não há uma regra nacional: confirme no site oficial da prefeitura ou no CRAS.',
        NULL, NULL, 'MUNICIPIO', 9);
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 1, 'Consulte o site da prefeitura',
        'Procure a área de serviços ao cidadão ou de assistência social no site oficial da sua cidade.');
INSERT INTO tb_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (seq_direito.CURRVAL, 2, 'Pergunte no CRAS',
        'O CRAS orienta sobre os programas sociais do município e ajuda na inscrição.');
INSERT INTO tb_direito_tag (cd_direito, tp_tag) VALUES (seq_direito.CURRVAL, 'TODOS');

COMMIT;
