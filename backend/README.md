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
| `token.segredo` | `MD_TOKEN_SEGREDO` | recomendada | sem segredo (ou com menos de 32 caracteres), a API gera um aleatório ao subir: as sessões caem a cada reinício |

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
| `ContainerResponseFilter` (`CabecalhosSegurancaFilter`) | Cabeçalhos de segurança e `Cache-Control` em toda resposta | Especificação JAX-RS |
| `javax.crypto.Mac` (HMAC-SHA256) + `Base64` | Token de sessão assinado, sem biblioteca de JWT | JDK |
| `ConcurrentHashMap` | Limite de tentativas de login em memória, sem tabela nova | JDK |
| `ExceptionMapper` | Tratamento de erro centralizado, em vez de try/catch repetido em cada rota | Especificação JAX-RS |
| `javax.crypto` PBKDF2 + `SecureRandom` | A senha nunca é gravada em texto puro (hash com sal, 120 mil iterações) | JDK |
| `java.util.Properties` + `System.getenv` | Tirar a senha do Oracle do código e do git | JDK |
| `java.util.logging` | Registrar erros inesperados no console (o cliente recebe só a mensagem amigável) | JDK |
| `addBatch` / `executeBatch` | Gravar o conteúdo de demonstração de uma cidade nova em ~1–2 s (antes ~7,5 s) | A mesma API JDBC do `PreparedStatement` |

Outras decisões:
- **Token de sessão assinado.** O login (`POST /auth/login`) e o cadastro (`POST /usuarios`) devolvem o usuário com o campo `token`, e o front o envia nas rotas 🔒 em `Authorization: Bearer <token>`. Formato: `base64url(id + "." + expiraEm) + "." + base64url(HMAC-SHA256(segredo, parte1))`, válido por **7 dias**. A assinatura é conferida em tempo constante e, em toda rota 🔒, a API confere também se o usuário ainda existe (conta excluída = 401). Sem o segredo (`token.segredo`), ninguém consegue trocar o id nem estender a validade. No Insomnia: faça o login, copie o `token` e use *Auth → Bearer Token*.
- **Limite de tentativas de login.** Depois de **5 falhas seguidas** no mesmo e-mail, o login desse e-mail fica bloqueado por **15 minutos** (HTTP **429**, "Muitas tentativas de login. Tente de novo em N minutos."). Um login certo zera o contador. Vale também para e-mails sem conta, e a resposta de senha errada continua genérica ("E-mail ou senha inválidos.") e com o mesmo tempo de resposta (hash fictício). Os contadores ficam em memória e as entradas vencidas são removidas periodicamente.
- **Imagens só de demonstração.** `imagemUrl` aceita apenas `img/demo/<nome>.jpg|jpeg|png|webp` (sem subpastas nem `..`, até 300 caracteres). Endereços externos (`https://...`) são recusados: uma imagem de outro site faria o navegador de quem vê a denúncia acessar um servidor de terceiros.
- **Cabeçalhos de segurança.** Toda resposta traz `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY` e `Cache-Control: no-store` (dados pessoais não ficam em cache). As listas fixas (`/categorias`, `/status-denuncia`, `/racas-cor`, `/deficiencias`, `/percursos-escolares`) são a exceção: `Cache-Control: public, max-age=86400`.
- **Consentimento no questionário.** `PUT /usuarios/perfil` só grava dados sensíveis (data de nascimento, raça/cor, deficiência, percurso escolar, baixa renda) com `consentimento: true`; sem ele, 400 no campo `consentimento`.
- **IPv4 e IPv6 em loopback.** Com `server.uri` em `localhost`/`127.0.0.1`, a API também escuta em `::1` na mesma porta. No Windows, navegadores e o Node tentam `::1` primeiro e esperariam ~2 s até cair para o IPv4. Se a máquina não tiver IPv6, a API só avisa no console.
- **Conexão por operação** com try-with-resources: nenhuma conexão fica aberta entre requisições.
- **"Cidade viva".** Na primeira conta de uma cidade, a API cria moradores e denúncias de exemplo (marcados como `simulado`), para que o avaliador veja votos, comentários e resolução funcionando. Esses moradores não fazem login, não aparecem nas listas de usuários e podem ser removidos com `DELETE /api/simulacao`.

## 6. Limitações conhecidas

- O token de sessão não pode ser revogado antes de vencer (7 dias): sair no front apaga o token do navegador, mas uma cópia continuaria válida. A exclusão da conta invalida o token na hora (a API confere se o usuário existe). O próximo passo seria uma lista de sessões no banco.
- O limite de tentativas de login fica em memória: zera ao reiniciar a API e não seria compartilhado entre várias instâncias.
- Não há perfis de usuário: a administração do catálogo de direitos usa uma chave fixa (`X-Chave-Admin`).
- A API abre uma conexão por requisição, sem pool. É suficiente para o MVP.
- A cidade é comparada por texto, ignorando acentos, maiúsculas e espaços repetidos ("Sao Paulo" = "São Paulo"). O próximo passo seria uma tabela de municípios do IBGE.
- Não há upload de imagem: a denúncia guarda o caminho de uma das fotos de demonstração do front (`img/demo/...`).
- Uma requisição `GET` com corpo é recusada pelo servidor Grizzly com 400 sem JSON, antes de chegar à API. Navegadores não enviam corpo em `GET`.

## 7. Uso de IA generativa (back-end)

Usamos o **Claude (Anthropic)**, via **Claude Code**, como apoio no back-end para:
- analisar o enunciado e o material das fases;
- planejar a arquitetura e o roadmap;
- gerar e revisar código;
- montar a bateria de testes de robustez e a coleção do Insomnia;
- documentar a API para o front-end.

Todo o código foi revisado, compreendido e testado pelos integrantes do back-end. Os testes foram feitos contra o Oracle da FIAP. As decisões técnicas e as justificativas estão documentadas nesta página.
