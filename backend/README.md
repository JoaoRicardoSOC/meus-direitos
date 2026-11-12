# Meus Direitos · Back-end

API REST em Java que atende o front-end React do projeto **Meus Direitos** (FIAP Global Solution 2026/2).
Ela grava e lê os dados de um banco **Oracle** via **JDBC**.

- **URL base:** `http://localhost:8080/api`
- **Contrato completo da API** (rotas, corpos, respostas, erros): [`docs/api.md`](../docs/api.md)
- **Coleção do Insomnia** (66 requisições, incluindo uma pasta só de erros): [`docs/insomnia/meus-direitos.insomnia.json`](../docs/insomnia/meus-direitos.insomnia.json)

---

## 1. Pré-requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 17 ou superior (o projeto compila com `release 17`) |
| Maven | 3.9 ou superior |
| Oracle Database | Acesso a um schema (ex.: o Oracle da FIAP) com as tabelas do `database/script.sql` criadas |

## 2. Configuração

1. Crie as tabelas rodando [`database/script.sql`](../database/script.sql) no SQL Developer.
2. Copie `db.properties.example` para `db.properties`, na pasta `backend/`, e preencha:

| Chave | Variável de ambiente | Obrigatória | Padrão |
|---|---|---|---|
| `db.url` | `MD_DB_URL` | não | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` |
| `db.usuario` | `MD_DB_USUARIO` | **sim** | — |
| `db.senha` | `MD_DB_SENHA` | **sim** | — |
| `server.uri` | `MD_SERVER_URI` | não | `http://localhost:8080/api/` |
| `cors.origens` | `MD_CORS_ORIGENS` | não | `http://localhost:5173` (o Vite) |
| `simulacao.ativa` | `MD_SIMULACAO_ATIVA` | não | `true` |
| `simulacao.chave` | `MD_SIMULACAO_CHAVE` | não | sem chave, as rotas `/api/simulacao/*` ficam desligadas |
| `admin.chave` | `MD_ADMIN_CHAVE` | não | sem chave, o catálogo de direitos fica somente leitura |

- A variável de ambiente tem prioridade sobre o arquivo.
- O `db.properties` está no `.gitignore`: **a senha do banco nunca vai para o repositório.**
- Se faltar usuário ou senha, a API avisa no console ao subir.

## 3. Como executar

Na pasta `backend/`:

```bash
mvn clean compile exec:java
```

- A API sobe em `http://localhost:8080/api`.
- Para conferir, abra `http://localhost:8080/api/health`: o campo `banco` vem `UP` quando a conexão com o Oracle funciona e `INDISPONIVEL` quando não.
- Para encerrar, use **Enter** ou **Ctrl+C**.

**Ordem para rodar o projeto inteiro:** `database/script.sql` → back-end (esta pasta) → front-end (`frontend/README.md`).

### Testes

```bash
mvn test
```

São 68 testes JUnit 5 que não precisam de banco. Eles cobrem:
- as regras de domínio (feed, resolução pela comunidade, tags do perfil);
- os validadores e o hash de senha;
- a tradução de erros do Oracle;
- o gerador de conteúdo de demonstração.

## 4. Arquitetura

Camadas, todas em `br.com.fiap.meusdireitos`:

```
resource  (JAX-RS: rotas, cabeçalhos, status HTTP)
   ↓
service   (regras de negócio, validação, controle de transação)
   ↓
dao       (interfaces) → dao.jdbc (SQL com PreparedStatement)
   ↓
factory   (ConnectionFactory → Oracle)
```

| Pacote | Conteúdo |
|---|---|
| `config` | `AppConfig` (configuração), `CorsFilter` |
| `factory` | `ConnectionFactory` |
| `model` | Entidades (`Usuario`, `Denuncia`, `Comentario`, `Direito`, `PerfilDemografico`…), `enums` e `relatorio` |
| `dto.request` / `dto.response` | Records de entrada e saída. A API nunca expõe a entidade, nem o hash da senha. |
| `dao` / `dao.jdbc` | Interfaces (`CrudDao<T>` genérico) e implementações JDBC. `Transacao` abre uma conexão por operação, com commit e rollback. |
| `service` | Regras de negócio. `validacao.Validador` acumula os erros de todos os campos de uma vez. `simulacao` gera o conteúdo de demonstração. |
| `resource` | Endpoints REST |
| `exception` / `exception.mapper` | Hierarquia de exceções com status HTTP e `ExceptionMapper`s que respondem sempre o mesmo JSON de erro |
| `util` | Hash de senha, texto, comparação de chaves |

**Tratamento de erros.** Toda falha vira o mesmo JSON (`status`, `erro`, `mensagem`, `campos`, `caminho`, `timestamp`), nunca uma página HTML ou um stack trace. Exemplos:
- JSON malformado → 400
- enum inválido → 400, com a lista de valores aceitos
- sem login → 401
- recurso de outra pessoa → 403
- inexistente → 404
- verbo errado → 405
- e-mail repetido → 409
- `Content-Type` errado → 415
- regra de negócio → 422
- banco fora do ar → 503

Esses casos foram testados manualmente, inclusive com o banco desligado: o servidor não cai. Para repetir os testes, use a pasta **"10 · Erros (robustez)"** da coleção do Insomnia (14 requisições que provocam os erros acima, exceto o 503, que exige desligar o banco).

**Regra de negócio central: o feed "Para você".**
- Relevância = saldo de votos, **+10** se a categoria afeta o perfil da pessoa (ex.: calçada para quem tem deficiência física), **−15** se a denúncia já foi resolvida.
- Uma denúncia é resolvida pelo autor ou por **3 confirmações** de moradores da mesma cidade.

## 5. Decisões técnicas

O projeto usa **apenas** as tecnologias do curso. O `pom.xml` tem só estas dependências:
- `jersey-container-grizzly2-http`;
- `jersey-hk2`;
- `jersey-media-json-binding`;
- `ojdbc8`;
- `junit-jupiter`.

Não há Spring, Hibernate/JPA, Lombok, Bean Validation nem pool de conexões.

Alguns recursos vêm da **própria especificação JAX-RS ou do JDK**, sem nenhuma dependência nova:

| Recurso | Para quê | Por que é permitido |
|---|---|---|
| `ContainerResponseFilter` (`CorsFilter`) | CORS: o React em `localhost:5173` chama a API em `:8080` | Faz parte da especificação JAX-RS já usada no projeto |
| `ExceptionMapper` | Tratamento de erro centralizado, em vez de try/catch repetido em cada rota | Especificação JAX-RS |
| `javax.crypto` PBKDF2 + `SecureRandom` | A senha nunca é gravada em texto puro (hash com sal, 120 mil iterações) | JDK |
| `java.util.Properties` + `System.getenv` | Tirar a senha do Oracle do código e do git | JDK |
| `java.util.logging` | Registrar erros inesperados no console (o cliente recebe só a mensagem amigável) | JDK |
| `addBatch` / `executeBatch` | Gravar o conteúdo de demonstração de uma cidade nova em ~1–2 s (antes ~7,5 s) | A mesma API JDBC do `PreparedStatement` |

Outras decisões:
- **Identificação por cabeçalho `X-Usuario-Id`.** O login devolve o usuário, e o front envia o id nas rotas 🔒. É simples de demonstrar e de testar no Insomnia (ver limitações).
- **Conexão por operação** com try-with-resources: nenhuma conexão fica aberta entre requisições.
- **"Cidade viva".** Na primeira conta de uma cidade, a API cria moradores e denúncias de exemplo (marcados como `simulado`), para que o avaliador veja votos, comentários e resolução funcionando. Esses moradores não fazem login, não aparecem nas listas de usuários e podem ser removidos com `DELETE /api/simulacao`.

## 6. Limitações conhecidas

- A identificação por `X-Usuario-Id` não é assinada: quem souber o id pode se passar pelo usuário. O próximo passo seria um token assinado (JWT) ou uma sessão.
- Não há perfis de usuário: a administração do catálogo de direitos usa uma chave fixa (`X-Chave-Admin`).
- A API abre uma conexão por requisição, sem pool. É suficiente para o MVP.
- A cidade é comparada por texto, ignorando acentos, maiúsculas e espaços repetidos ("Sao Paulo" = "São Paulo"). O próximo passo seria uma tabela de municípios do IBGE.
- Não há upload de imagem: a denúncia guarda o caminho de uma imagem do front ou um endereço `https://`.
- Uma requisição `GET` com corpo é recusada pelo servidor Grizzly com 400 sem JSON, antes de chegar à API. Navegadores não enviam corpo em `GET`.

## 7. Uso de IA generativa (back-end)

Usamos o **Claude (Anthropic)**, via **Claude Code**, como apoio no back-end para:
- analisar o enunciado e o material das fases;
- planejar a arquitetura e o roadmap;
- gerar e revisar código;
- montar a bateria de testes de robustez e a coleção do Insomnia;
- documentar a API para o front-end.

Todo o código foi revisado, compreendido e testado pelos integrantes do back-end. Os testes foram feitos contra o Oracle da FIAP. As decisões técnicas e as justificativas estão documentadas nesta página.
