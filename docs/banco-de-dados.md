# Banco de dados: guia de operação

Como criar, conferir, evoluir e limpar o banco do projeto. O modelo (tabelas, colunas e relacionamentos) está no [diagrama ER](diagramas/diagrama-er.md).

## 1. O que é preciso saber antes

> **O schema do Oracle da FIAP é compartilhado com outro projeto.** Ele tem tabelas que não são nossas e que não podem ser alteradas: as do challenge, com o prefixo `TB_CH26_` (`TB_CH26_PRODUTO`, `TB_CH26_SESSAO`…), `TODOS` e `USERS`.
>
> **Padrão de nomes.** Todo objeto deste projeto leva `GS26S2` (Global Solution 2026, 2º semestre) depois do prefixo do tipo: `TB_GS26S2_<TABELA>`, `SEQ_GS26S2_<TABELA>`, `PK_GS26S2_<TABELA>`, `FK_GS26S2_<TABELA>_<REFERENCIADA>`, `UK_`/`CK_`/`IX_GS26S2_<TABELA>_<COLUNA>`. As colunas não levam o prefixo. Assim os objetos do projeto se distinguem no schema e não conflitam com os de outros trabalhos.

Objetos deste projeto, e somente estes:

| Tipo | Nomes |
|---|---|
| Tabelas | `TB_GS26S2_USUARIO`, `TB_GS26S2_PERFIL_DEMOGRAFICO`, `TB_GS26S2_DENUNCIA`, `TB_GS26S2_COMENTARIO`, `TB_GS26S2_VOTO_DENUNCIA`, `TB_GS26S2_CONFIRMACAO_RESOLUCAO`, `TB_GS26S2_REPORTE_COMENTARIO`, `TB_GS26S2_DIREITO`, `TB_GS26S2_DIREITO_PASSO`, `TB_GS26S2_DIREITO_TAG`, `TB_GS26S2_SEMEADURA_CIDADE` |
| Sequences | `SEQ_GS26S2_USUARIO`, `SEQ_GS26S2_DENUNCIA`, `SEQ_GS26S2_COMENTARIO`, `SEQ_GS26S2_DIREITO` |
| Índices | `IX_GS26S2_USUARIO_SIMULADO`, `IX_GS26S2_USUARIO_CIDADE_CHAVE`, `IX_GS26S2_DENUNCIA_CIDADE`, `IX_GS26S2_DENUNCIA_CIDADE_CHAVE`, `IX_GS26S2_DENUNCIA_USUARIO`, `IX_GS26S2_COMENTARIO_DENUNCIA`, `IX_GS26S2_COMENTARIO_USUARIO`, `IX_GS26S2_VOTO_DENUNCIA_DENUNCIA`, `IX_GS26S2_CONFIRMACAO_RESOLUCAO_DENUNCIA`, `IX_GS26S2_REPORTE_COMENTARIO_COMENTARIO`, `IX_GS26S2_DIREITO_TAG_TAG` (mais os criados pelas chaves primárias e únicas, como `PK_GS26S2_USUARIO` e `UK_GS26S2_USUARIO_EMAIL`) |

## 2. Criar o banco

O [`database/script.sql`](../database/script.sql) monta tudo e **nunca apaga nada**. Na etapa 0, num schema criado com os nomes antigos (`TB_USUARIO`, `SEQ_USUARIO`, `PK_USUARIO`…), renomeia cada objeto deste projeto para o padrão `GS26S2`, mantendo dados, chaves e índices. Depois, cada objeto só é criado se ainda não existir, e cada tema do catálogo só é inserido se o título ainda não estiver cadastrado. Ele pode rodar quantas vezes for preciso.

1. Abra o SQL Developer conectado ao schema que a API usa (o `db.usuario` do `backend/db.properties`).
2. Abra o `database/script.sql` e use **Executar Script (F5)**, não "Executar Instrução" (Ctrl+Enter).
3. Confira a aba **Saída do Script**. Cada bloco escreve o que fez, por exemplo `Criada a tabela TB_GS26S2_REPORTE_COMENTARIO` ou `TB_GS26S2_USUARIO já existe: mantida como está`.

O script está dividido em quatro partes:

| Parte | Conteúdo |
|---|---|
| 1. Sequences | `SEQ_GS26S2_USUARIO`, `SEQ_GS26S2_DENUNCIA`, `SEQ_GS26S2_COMENTARIO`, `SEQ_GS26S2_DIREITO` |
| 2. Tabelas | Na ordem das chaves estrangeiras, com `CHECK` nos valores de lista e nos booleanos |
| 3. Índices | Índices comuns e os índices baseados em função da busca por cidade |
| 4. Carga inicial | Os 5 temas do catálogo de direitos, com passos e públicos |

## 3. Conferir se está tudo certo

```sql
-- Tabelas do projeto (devem ser 11)
SELECT table_name FROM user_tables
 WHERE table_name IN ('TB_GS26S2_USUARIO','TB_GS26S2_PERFIL_DEMOGRAFICO','TB_GS26S2_DENUNCIA','TB_GS26S2_COMENTARIO','TB_GS26S2_VOTO_DENUNCIA',
                      'TB_GS26S2_CONFIRMACAO_RESOLUCAO','TB_GS26S2_REPORTE_COMENTARIO','TB_GS26S2_DIREITO','TB_GS26S2_DIREITO_PASSO',
                      'TB_GS26S2_DIREITO_TAG','TB_GS26S2_SEMEADURA_CIDADE')
 ORDER BY table_name;

-- Catálogo de direitos (5 temas na carga inicial)
SELECT cd_direito, ds_titulo FROM tb_gs26s2_direito ORDER BY nr_ordem;

-- Pessoas reais e moradores de exemplo por cidade
SELECT nm_cidade, sg_uf, st_simulado, COUNT(*) FROM tb_gs26s2_usuario GROUP BY nm_cidade, sg_uf, st_simulado ORDER BY 1;
```

Com a API rodando, `GET http://localhost:8080/api/health` responde `"banco":"UP"` quando a conexão funciona.

## 4. Evoluir o banco

Toda mudança de estrutura entra no `database/script.sql`, no mesmo padrão dos blocos que já existem: verificar no dicionário de dados e criar só se não existir.

**Tabela nova** (depois das tabelas de que ela depende):

```sql
DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tables WHERE table_name = 'TB_GS26S2_EXEMPLO';
  IF existe = 0 THEN
    EXECUTE IMMEDIATE q'[
    CREATE TABLE TB_GS26S2_EXEMPLO
       (    CD_EXEMPLO NUMBER(10,0) NOT NULL,
        CD_USUARIO NUMBER(10,0) NOT NULL,
        DT_CRIACAO TIMESTAMP (6) DEFAULT SYSTIMESTAMP NOT NULL,
         CONSTRAINT PK_GS26S2_EXEMPLO PRIMARY KEY (CD_EXEMPLO),
         CONSTRAINT FK_GS26S2_EXEMPLO_USUARIO FOREIGN KEY (CD_USUARIO) REFERENCES TB_GS26S2_USUARIO (CD_USUARIO)
       )]';
    DBMS_OUTPUT.PUT_LINE('Criada a tabela TB_GS26S2_EXEMPLO');
  ELSE
    DBMS_OUTPUT.PUT_LINE('TB_GS26S2_EXEMPLO já existe: mantida como está');
  END IF;
END;
/
```

**Índice novo:** o mesmo padrão, consultando `user_indexes WHERE index_name = '...'`.

**Coluna nova numa tabela existente:** consulte `user_tab_columns` e use `ALTER TABLE ... ADD`, sempre com valor padrão ou permitindo nulo, para não quebrar as linhas que já existem. Nunca remova nem renomeie colunas pelo script.

Depois de mudar o banco:

1. Atualize o [diagrama ER](diagramas/diagrama-er.md).
2. Se a tabela tem chave estrangeira para usuário, denúncia ou comentário, inclua a exclusão dela antes da tabela mãe em `UsuarioService.excluirConta`, `DenunciaService.excluir`, `ComentarioService.excluir` e `SimulacaoService.limparTudo` (o projeto não usa `ON DELETE CASCADE`).
3. Rode os testes de integração: `MD_INTEGRACAO=true mvn test -Dtest=DaosIntegracaoTest`.

### Convenções de nomes

| Prefixo | Significado | Exemplo |
|---|---|---|
| `TB_GS26S2_` | Tabela do projeto | `TB_GS26S2_DENUNCIA` |
| `SEQ_GS26S2_` | Sequence do projeto | `SEQ_GS26S2_DENUNCIA` |
| `CD_` | Código (chave) | `CD_USUARIO` |
| `DS_` / `NM_` | Descrição / nome | `DS_TITULO`, `NM_CIDADE` |
| `SG_` | Sigla | `SG_UF` |
| `DT_` | Data e hora | `DT_CRIACAO` |
| `TP_` | Tipo (valor de lista, com `CHECK`) | `TP_CATEGORIA` |
| `ST_` | Situação ou booleano (`'S'`/`'N'`) | `ST_SIMULADO` |
| `NR_` / `QT_` | Número / quantidade | `NR_ORDEM`, `QT_EVENTOS` |
| `PK_GS26S2_` / `FK_GS26S2_` | Chave primária / estrangeira (FK: tabela + referenciada) | `PK_GS26S2_DENUNCIA`, `FK_GS26S2_DENUNCIA_USUARIO` |
| `UK_GS26S2_` / `CK_GS26S2_` / `IX_GS26S2_` | Única / check / índice (tabela + coluna) | `UK_GS26S2_USUARIO_EMAIL`, `IX_GS26S2_DENUNCIA_USUARIO` |

Os valores de lista são gravados com o nome da constante do enum Java (`ASFALTO`, `EM_ANDAMENTO`…), e o `CHECK` da tabela lista os mesmos valores.

## 5. Conteúdo de demonstração ("cidade viva")

A API cria moradores e denúncias de exemplo para a primeira pessoa de cada cidade (ver a [arquitetura](arquitetura.md#44-cidade-viva-conteúdo-de-demonstração)). Esses dados ficam nas mesmas tabelas, marcados com `TB_GS26S2_USUARIO.ST_SIMULADO = 'S'`, e cada cidade preparada tem uma linha em `TB_GS26S2_SEMEADURA_CIDADE`.

Com `simulacao.chave` configurada no `db.properties`, as rotas `/api/simulacao/*` (cabeçalho `X-Chave-Simulacao`) permitem preparar uma cidade antes de uma apresentação (`POST /simulacao/cidades`), gerar atividade (`POST /simulacao/pulso`) e **apagar todo o conteúdo de demonstração** (`DELETE /simulacao`), sem tocar nas contas reais. Os exemplos estão na pasta "09 · Demonstração" da coleção do Insomnia.

Para desligar a geração de conteúdo novo, use `simulacao.ativa=false` (ou `MD_SIMULACAO_ATIVA=false`).

## 6. Problemas comuns

| Mensagem | Causa provável | O que fazer |
|---|---|---|
| `ORA-00942: tabela ou view não existe` | O script não foi executado neste schema, ou a API está conectada a outro usuário | Rode o script conectado ao `db.usuario` da API |
| `ORA-01017: nome do usuário/senha inválido` | Credenciais erradas no `db.properties` | Confira `db.usuario` e `db.senha` |
| A API responde 503 "banco indisponível" | Sem rede até `oracle.fiap.com.br`, ou todas as conexões do pool ocupadas por mais de 10 s | Confira a rede/VPN e o `GET /api/health` |
| `ORA-02292` ao excluir algo | Uma tabela filha não foi apagada antes | Inclua a exclusão da tabela filha no service (seção 4) |
| `ORA-12899` (valor muito grande) | Texto no limite de caracteres, mas cheio de acentos, passou do limite em bytes | A API já responde 400 no campo; encurte o texto |
| `ORA-01743` ao criar um índice | A expressão usa uma função que o Oracle não indexa (ex.: `REGEXP_REPLACE`) | Use só funções puras na expressão indexada |
