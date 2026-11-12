# Modelo de dados (diagrama ER)

Gerado a partir de [`database/script.sql`](../../database/script.sql), que cria estas tabelas no Oracle.
O GitHub desenha o diagrama abaixo direto nesta página.

## Diagrama

```mermaid
erDiagram
    TB_USUARIO ||--o| TB_PERFIL_DEMOGRAFICO : "responde"
    TB_USUARIO ||--o{ TB_DENUNCIA : "registra"
    TB_USUARIO ||--o{ TB_COMENTARIO : "escreve"
    TB_USUARIO ||--o{ TB_VOTO_DENUNCIA : "vota"
    TB_USUARIO ||--o{ TB_CONFIRMACAO_RESOLUCAO : "confirma"
    TB_DENUNCIA ||--o{ TB_COMENTARIO : "recebe"
    TB_DENUNCIA ||--o{ TB_VOTO_DENUNCIA : "recebe"
    TB_DENUNCIA ||--o{ TB_CONFIRMACAO_RESOLUCAO : "recebe"
    TB_DIREITO ||--|{ TB_DIREITO_PASSO : "tem"
    TB_DIREITO ||--|{ TB_DIREITO_TAG : "aplica-se a"

    TB_USUARIO {
        NUMBER cd_usuario PK
        VARCHAR2 nm_usuario
        VARCHAR2 ds_email UK
        VARCHAR2 ds_senha_hash
        VARCHAR2 nm_cidade
        CHAR sg_uf
        TIMESTAMP dt_cadastro
        CHAR st_simulado "S/N"
    }

    TB_PERFIL_DEMOGRAFICO {
        NUMBER cd_usuario PK,FK
        DATE dt_nascimento "opcional"
        VARCHAR2 tp_raca_cor "opcional"
        VARCHAR2 tp_deficiencia "opcional"
        VARCHAR2 tp_percurso_escolar "opcional"
        CHAR st_baixa_renda "S/N, opcional"
        TIMESTAMP dt_atualizacao
    }

    TB_DENUNCIA {
        NUMBER cd_denuncia PK
        NUMBER cd_usuario FK "autor"
        VARCHAR2 ds_titulo
        VARCHAR2 ds_denuncia
        VARCHAR2 tp_categoria
        VARCHAR2 st_denuncia
        VARCHAR2 ds_endereco
        VARCHAR2 nm_bairro "opcional"
        VARCHAR2 nm_cidade
        CHAR sg_uf
        VARCHAR2 ds_imagem_url "opcional"
        TIMESTAMP dt_criacao
        TIMESTAMP dt_atualizacao
    }

    TB_COMENTARIO {
        NUMBER cd_comentario PK
        NUMBER cd_denuncia FK
        NUMBER cd_usuario FK
        VARCHAR2 ds_comentario
        TIMESTAMP dt_criacao
        TIMESTAMP dt_edicao "opcional"
    }

    TB_VOTO_DENUNCIA {
        NUMBER cd_usuario PK,FK
        NUMBER cd_denuncia PK,FK
        VARCHAR2 tp_voto "UP/DOWN"
        TIMESTAMP dt_voto
    }

    TB_CONFIRMACAO_RESOLUCAO {
        NUMBER cd_usuario PK,FK
        NUMBER cd_denuncia PK,FK
        TIMESTAMP dt_confirmacao
    }

    TB_DIREITO {
        NUMBER cd_direito PK
        VARCHAR2 ds_titulo UK
        VARCHAR2 ds_resumo
        VARCHAR2 ds_direito
        VARCHAR2 ds_aviso "opcional"
        VARCHAR2 ds_base_legal "opcional"
        VARCHAR2 ds_link_oficial "opcional"
        VARCHAR2 tp_icone
        NUMBER nr_ordem
    }

    TB_DIREITO_PASSO {
        NUMBER cd_direito PK,FK
        NUMBER nr_ordem PK
        VARCHAR2 ds_titulo
        VARCHAR2 ds_passo
    }

    TB_DIREITO_TAG {
        NUMBER cd_direito PK,FK
        VARCHAR2 tp_tag PK
    }

    TB_SEMEADURA_CIDADE {
        VARCHAR2 nm_cidade_chave PK
        CHAR sg_uf PK
        VARCHAR2 nm_cidade
        TIMESTAMP dt_semeadura
        TIMESTAMP dt_ultimo_pulso
        NUMBER qt_eventos
    }
```

`TB_SEMEADURA_CIDADE` não tem chave estrangeira: ela registra quais cidades já receberam o conteúdo de demonstração ("cidade viva") e quando foi o último pulso de atividade. A ligação com usuários e denúncias é pelo nome da cidade e pela UF.

## Regras garantidas pelo banco

| Regra | Como |
|---|---|
| Um e-mail por conta | `UNIQUE (ds_email)` em `tb_usuario` (a API responde 409) |
| Um voto por pessoa em cada denúncia | Chave primária `(cd_usuario, cd_denuncia)` em `tb_voto_denuncia` |
| Uma confirmação de resolução por pessoa | Chave primária `(cd_usuario, cd_denuncia)` em `tb_confirmacao_resolucao` |
| Questionário opcional, no máximo um por usuário | `cd_usuario` é a chave primária e estrangeira de `tb_perfil_demografico` |
| Título de tema sem repetição | `UNIQUE (ds_titulo)` em `tb_direito` (a API também compara sem diferenciar maiúsculas antes de gravar) |
| Uma semeadura por cidade | Chave primária `(nm_cidade_chave, sg_uf)` em `tb_semeadura_cidade` |
| Valores de lista válidos | `CHECK` em categoria, status, voto, raça/cor, deficiência, percurso escolar, ícone e tag, com os mesmos valores dos enums Java (a UF é validada pela API) |
| Passos numerados de 1 a 10 | `CHECK (nr_ordem BETWEEN 1 AND 10)` em `tb_direito_passo` |
| Booleanos | `CHAR(1)` com `CHECK IN ('S', 'N')` |

## Decisões

- **Script que só cria o que falta.** O `database/script.sql` reproduz exatamente as tabelas do schema usado no Oracle da FIAP e nunca apaga nem altera nada: cada tabela, sequence e índice só é criado se ainda não existir, e cada tema do catálogo só é inserido se o título ainda não estiver cadastrado. Assim ele pode rodar num schema compartilhado com outros projetos.
- **Datas com hora em `TIMESTAMP`** com `DEFAULT SYSTIMESTAMP`; a data de nascimento é `DATE`.
- **Textos em `VARCHAR2` com o limite padrão do schema (em bytes).** A API valida os tamanhos em caracteres. Um texto no limite máximo e cheio de acentos pode passar de bytes: nesse caso o Oracle recusa (ORA-12899) e a API responde 400 indicando o campo.
- **Sem `ON DELETE CASCADE`.** A API apaga as tabelas filhas antes da tabela mãe, na mesma transação. Se algo escapar, o Oracle recusa a exclusão (ORA-02292) e a API responde 409 em vez de apagar dados em silêncio.
- **Chaves geradas por sequence** (`seq_usuario`, `seq_denuncia`, `seq_comentario`, `seq_direito`), usadas pela API com `NEXTVAL`.
- **Cidade guardada como texto.** A comparação ignora acentos, maiúsculas e espaços repetidos (`TRANSLATE(UPPER(...))` nas consultas). O próximo passo seria uma tabela de municípios do IBGE (ver as limitações no [README do back-end](../../backend/README.md#6-limitações-conhecidas)).
