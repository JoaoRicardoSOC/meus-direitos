# Meus Direitos · Back-end

API REST em Java que atende o front-end React do projeto **Meus Direitos** (FIAP Global Solution 2026/2).
Ela grava e lê os dados de um banco **Oracle** via **JDBC**.

- **URL base:** `http://localhost:8080/api`
- **Contrato completo da API** (rotas, corpos, respostas, erros): [`docs/api.md`](../docs/api.md)
- **Coleção do Insomnia** (69 requisições, incluindo uma pasta só de erros): [`docs/insomnia/meus-direitos.insomnia.json`](../docs/insomnia/meus-direitos.insomnia.json)

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

- A variável de ambiente tem prioridade sobre o arquivo; sem nenhum dos dois, vale o padrão da tabela (`AppConfig`).
- O `db.properties` é lido da **pasta onde a API é executada**: rode os comandos dentro de `backend/`.
- O tamanho do pool de conexões vem só da variável de ambiente `MD_DB_POOL` (padrão `5`, de 1 a 50).
- O `db.properties` está no `.gitignore`: **a senha do banco nunca vai para o repositório.**
- Se faltar usuário ou senha, a API sobe mesmo assim e avisa no console; as rotas que usam o banco respondem 503 e o `/health` mostra `"banco":"INDISPONIVEL"`.
- **CORS:** só as origens de `cors.origens` recebem os cabeçalhos CORS (métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`; cabeçalhos `Content-Type` e `Authorization`; o `Location` fica visível para o front). Para abrir o `frontend/dist/index.html` direto do disco (`file://`), acrescente `null`: `cors.origens=http://localhost:5173,null`.

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

São **128 testes em 24 classes** (JUnit 5). Sem banco, o `mvn test` roda **122** e marca os 6 de integração como
ignorados (*skipped*). Os testes de unidade não precisam de banco. Eles cobrem:
- as regras de domínio (feed, resolução pela comunidade, tags do perfil, relatórios, enums);
- os validadores, o hash de senha e a comparação das chaves de administração;
- o token de sessão (assinatura, validade, adulteração) e a leitura do cabeçalho `Authorization: Bearer`;
- o limite de tentativas de login (bloqueio, desbloqueio, contadores vencidos);
- o consentimento exigido no questionário;
- os cabeçalhos de segurança e o `Cache-Control`;
- a API escutando em IPv4 e IPv6 no loopback (sobe um servidor de teste numa porta livre);
- a tradução de erros do Oracle;
- o gerador de conteúdo de demonstração;
- o pool de conexões (com uma `Connection` falsa, sem banco);
- o filtro de termos ofensivos dos comentários;
- a igualdade entre a expressão de cidade das consultas e a dos índices do `script.sql`.

| Classe de teste | Testes | O que confere |
|---|---|---|
| `PoolConexoesTest` | 15 | Empréstimo, devolução com ROLLBACK, conexão derrubada descartada, espera esgotada (503) |
| `ValidadorTest` | 12 | Regras de cada campo, acúmulo de erros, fotos `img/demo/` |
| `DenunciaTest`, `TokenUtilTest`, `LimiteTentativasLoginTest` | 9 cada | Regras da denúncia; token; limite de login |
| `PerfilDemograficoTest`, `GeradorConteudoTest` | 7 cada | Idade e tags do perfil; conteúdo da "cidade viva" |
| `PersistenciaExceptionTest` | 6 | Códigos ORA → status HTTP |
| `AutenticacaoServiceTest`, `SenhaUtilTest`, `FiltroTermosOfensivosTest` | 5 cada | Cabeçalho Bearer, token inválido (401) e `idOpcional`; hash PBKDF2; termos ofensivos |
| `CategoriaDenunciaTest`, `StatusDenunciaTest`, `RelatorioTest`, `TextoUtilTest` | 4 cada | Categorias relevantes por perfil; transições de status; percentuais; normalização de texto |
| `JdbcUtilTest`, `PerfilServiceTest` | 3 cada | IN dinâmico e expressão de cidade; consentimento e data de nascimento |
| `CabecalhosSegurancaFilterTest`, `MainLoopbackTest`, `UsuarioResponseTest`, `PerfilEnumsTest`, `ChaveUtilTest` | 2 cada | Cabeçalhos e `Cache-Control`; IPv4 + IPv6 só em loopback; `token` omitido do JSON quando nulo; respostas do perfil que geram tags; chaves |
| `IndiceCidadeScriptTest` | 1 | Índices do `script.sql` iguais à expressão das consultas |
| `DaosIntegracaoTest` | 6 | Integração com o Oracle (abaixo) |

#### Testes de integração (Oracle)

`src/test/java/.../integracao/DaosIntegracaoTest` testa os DAOs contra o Oracle de verdade: usuário, denúncia no
feed e na busca com a cidade escrita sem acento, votos e placar, comentário oculto após 3 reportes, resumo da
cidade e a ordem de exclusão da conta. **Nada fica gravado:** cada teste usa uma única conexão com
`autoCommit=false` e termina com ROLLBACK (só as sequences avançam). Os dados usam uma cidade fictícia e e-mails
com sufixo aleatório.

Eles só rodam com a variável `MD_INTEGRACAO=true` (e com o `db.properties` preenchido); sem ela, o `mvn test`
os marca como ignorados:

```bash
MD_INTEGRACAO=true mvn test                          # Git Bash / Linux / macOS
$env:MD_INTEGRACAO="true"; mvn test                  # PowerShell
MD_INTEGRACAO=true mvn test -Dtest=DaosIntegracaoTest   # só os de integração
```

O GitHub Actions (`.github/workflows/ci.yml`) roda `mvn -B -q test` a cada push e pull request, sem os testes de
integração. Há também um teste de ponta a ponta da API, `testes/api-ponta-a-ponta.mjs` (só precisa do Node 22 e
da API no ar), descrito no README da raiz.

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
| *(raiz)* | `Main`: sobe o Grizzly com o Jersey, que encontra sozinho os resources (`@Path`) e os providers (`@Provider`) do pacote; com `localhost`, escuta também no loopback IPv6 |
| `config` | `AppConfig` (configuração), `CorsFilter` (CORS) e `CabecalhosSegurancaFilter` (cabeçalhos de segurança e `Cache-Control`) |
| `factory` | `ConnectionFactory` (credenciais e tamanho do pool) e `PoolConexoes` (pool feito só com o JDK) |
| `model` | Entidades (`Usuario`, `Denuncia`, `Comentario`, `Direito`, `PerfilDemografico`, `VotoDenuncia`…) e as regras que ficam nelas; `enums` (categoria, status, tags…) e `relatorio` (totais dos relatórios) |
| `dto.request` / `dto.response` | Records de entrada e saída. A API nunca expõe a entidade, nem o hash da senha. |
| `dao` | Interfaces (`CrudDao<T>` genérico e as específicas, como `ReporteComentarioDao`), `FiltroDenuncia` e `Transacao` (uma conexão por operação, com commit e rollback) |
| `dao.jdbc` | Implementações `*DaoJdbc` com `PreparedStatement`, e `JdbcUtil` (conversões e a comparação de cidade) |
| `service` | Regras de negócio e controle de transação. `AutenticacaoService` (login e token) usa `LimiteTentativasLogin`. Subpacotes: `validacao` (`Validador`, que acumula os erros de todos os campos), `moderacao` (`FiltroTermosOfensivos`) e `simulacao` ("cidade viva") |
| `resource` | Endpoints REST: `AuthResource`, `UsuarioResource`, `PerfilResource`, `DireitoResource`, `DenunciaResource`, `ComentarioResource`, `RelatorioResource`, `MetadadosResource`, `HealthResource` e `SimulacaoResource` |
| `exception` / `exception.mapper` | Hierarquia de exceções com status HTTP e `ExceptionMapper`s que respondem sempre o mesmo JSON de erro |
| `util` | `SenhaUtil` (hash PBKDF2), `TokenUtil` (token HMAC), `TextoUtil` (normalização de cidade e texto), `ChaveUtil` (comparação das chaves em tempo constante) e `GeradorHashSenha` (gera hashes de senha para scripts, pela linha de comando) |

**Caminho de um pedido.** Exemplo: `POST /api/denuncias/7/comentarios` com `{"texto": "..."}`.
1. O Jersey converte o JSON no record `ComentarioRequest` (JSON-B) e chama `DenunciaResource.comentar`.
2. O resource só cuida do HTTP: pede à `AutenticacaoService.idDoUsuarioLogado` o id que está no token
   (`Authorization: Bearer`) e repassa o pedido ao `ComentarioService.criar`.
3. O service valida o pedido (`Validador`: tamanho e termos ofensivos) e abre a transação com
   `Transacao.executar`, que pega uma conexão do pool, desliga o autocommit e faz COMMIT no fim (ou ROLLBACK se
   algo falhar).
4. Dentro da transação, os DAOs JDBC (`DenunciaDaoJdbc`, `ComentarioDaoJdbc`) executam o SQL com `PreparedStatement`.
5. O service devolve um DTO de resposta (`ComentarioResponse`), e o resource responde 201 com o cabeçalho `Location`.
6. Se qualquer camada lançar uma exceção, um `ExceptionMapper` monta a resposta de erro (abaixo). Por fim, os
   filtros `CorsFilter` e `CabecalhosSegurancaFilter` acrescentam os cabeçalhos a toda resposta, inclusive às de erro.

Rotas públicas que mostram algo a mais para quem está logado (o próprio voto, `meu` nos comentários, a cidade dos
relatórios) usam `AutenticacaoService.idOpcional`, que confere o token e se a conta ainda existe e nunca responde 401: token inválido, vencido ou de conta excluída conta como visitante.

**Tratamento de erros.** Toda falha vira o mesmo JSON (`status`, `erro`, `mensagem`, `campos`, `caminho`, `timestamp`), nunca uma página HTML ou um stack trace. As rotas não têm try/catch: as exceções sobem até um dos quatro `ExceptionMapper`s.

| Mapper | Trata | Resposta |
|---|---|---|
| `MeusDireitosExceptionMapper` | As exceções da aplicação (`MeusDireitosException` e filhas). Cada uma informa o próprio status: `ValidacaoException` 400, `NaoAutenticadoException` 401, `AcessoNegadoException` 403, `EntidadeNaoEncontradaException` 404, `ConflitoException` 409, `RegraNegocioException` 422, `MuitasTentativasException` 429, `PersistenciaException` 500 ou 503 | Status da exceção, com os erros por campo em `campos` |
| `JsonInvalidoMapper` | Corpo que o JSON-B não consegue converter: JSON malformado, valor de enum inexistente, data fora de `AAAA-MM-DD` | 400; no enum inválido, a mensagem lista os valores aceitos |
| `WebApplicationExceptionMapper` | Erros do próprio Jersey: rota inexistente, método errado, `Content-Type` errado | Mantém o status (404, 405, 415…) e o cabeçalho `Allow` |
| `ErroInesperadoMapper` | Qualquer outra exceção | 500 com mensagem genérica; o stack trace vai só para o log |

Os erros do Oracle passam por `PersistenciaException.de(SQLException)`: registro duplicado e vínculo que impede a
exclusão viram 409, valor grande demais vira 400, banco fora do ar (ou pool sem conexão livre) vira 503.

Exemplos:
- JSON malformado → 400
- enum inválido → 400, com a lista de valores aceitos
- sem login → 401
- recurso de outra pessoa → 403
- inexistente → 404
- verbo errado → 405
- e-mail repetido → 409
- `Content-Type` errado → 415
- regra de negócio → 422
- login bloqueado por muitas tentativas → 429
- banco fora do ar → 503

Esses casos foram testados manualmente, inclusive com o banco desligado: o servidor não cai. Para repetir os testes, use a pasta **"10 · Erros (robustez)"** da coleção do Insomnia (16 requisições que provocam os erros acima, exceto o 429, que exige errar a senha 5 vezes, e o 503, que exige desligar o banco).

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

Não há Spring, Hibernate/JPA, Lombok, Bean Validation nem biblioteca de pool de conexões (o pool é feito com o JDK, ver abaixo).

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
| `BlockingQueue` + `Semaphore` | Pool de conexões: reaproveitar as conexões com o Oracle em vez de abrir uma por requisição | JDK |

Outras decisões:
- **Token de sessão assinado.** O login (`POST /auth/login`) e o cadastro (`POST /usuarios`) devolvem o usuário com o campo `token`, e o front o envia nas rotas 🔒 em `Authorization: Bearer <token>`. Formato: `base64url(id + "." + expiraEm) + "." + base64url(HMAC-SHA256(segredo, parte1))`, válido por **7 dias**. A assinatura é conferida em tempo constante e, em toda rota 🔒, a API confere também se o usuário ainda existe (conta excluída = 401). Nas rotas públicas, um token inválido é apenas ignorado. O token não é criptografado: o id do usuário fica legível, o que não é segredo. Sem o segredo (`token.segredo`), ninguém consegue trocar o id nem estender a validade. No Insomnia: faça o login, copie o `token` e use *Auth → Bearer Token*.
- **Limite de tentativas de login.** Depois de **5 falhas seguidas** no mesmo e-mail, o login desse e-mail fica bloqueado por **15 minutos** (HTTP **429**, "Muitas tentativas de login. Tente de novo em N minutos."). Um login certo zera o contador. Vale também para e-mails sem conta, e a resposta de senha errada continua genérica ("E-mail ou senha inválidos.") e com o mesmo tempo de resposta (hash fictício). Os contadores ficam em memória e as entradas vencidas são removidas periodicamente.
- **Imagens só de demonstração.** `imagemUrl` aceita apenas `img/demo/<nome>.jpg|jpeg|png|webp` (sem subpastas nem `..`, até 300 caracteres). Endereços externos (`https://...`) são recusados: uma imagem de outro site faria o navegador de quem vê a denúncia acessar um servidor de terceiros.
- **Cabeçalhos de segurança.** Toda resposta traz `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY` e `Cache-Control: no-store` (dados pessoais não ficam em cache). As listas fixas (`/categorias`, `/status-denuncia`, `/racas-cor`, `/deficiencias`, `/percursos-escolares`) são a exceção: `Cache-Control: public, max-age=86400`.
- **Consentimento no questionário.** `PUT /usuarios/perfil` só grava dados sensíveis (data de nascimento, raça/cor, deficiência, percurso escolar, baixa renda) com `consentimento: true`; sem ele, 400 no campo `consentimento`.
- **IPv4 e IPv6 em loopback.** Com `server.uri` em `localhost`/`127.0.0.1`, a API também escuta em `::1` na mesma porta. No Windows, navegadores e o Node tentam `::1` primeiro e esperariam ~2 s até cair para o IPv4. Se a máquina não tiver IPv6, a API só avisa no console.
- **Pool de conexões só com o JDK** (`factory.PoolConexoes`). Abrir uma conexão com o Oracle da FIAP custa
  ~300 ms, e uma consulta ~30 ms (medido em 13/11). Sem pool, cada requisição pagaria a abertura. Agora até
  `MD_DB_POOL` conexões (padrão 5) ficam abertas numa `BlockingQueue` e são reaproveitadas:
  - `ConnectionFactory.emprestar()` entrega uma conexão livre (testada com `isValid(2)`; se o banco a
    derrubou, é descartada e outra é aberta) ou abre uma nova se ainda houver vaga (`Semaphore`);
  - com todas em uso, espera até 10 s e então responde 503 ("banco indisponível"), sem travar o servidor;
  - `ConnectionFactory.devolver(con)` desfaz o que ficou sem COMMIT (ROLLBACK), volta ao autocommit e guarda a
    conexão na fila. A `Transacao` (e o health check) devolve sempre num bloco `finally`; os DAOs não mudam;
  - `ConnectionFactory.getConnection()` continua abrindo uma conexão nova, como no Cap17 (usada no teste de
    integração).
- **Índice para a busca por cidade.** A cidade é comparada sem acentos nem maiúsculas
  (`JdbcUtil.cidadeIgual`: `TRANSLATE(UPPER(TRIM(...)))`), e a API junta os espaços repetidos antes de gravar.
  Assim a expressão só usa funções puras: com `REGEXP_REPLACE` o Oracle recusa o índice (ORA-01743).
  O `script.sql` cria índices baseados em função com exatamente essa expressão, mais a
  UF, em `TB_DENUNCIA` e `TB_USUARIO` (`IX_DENUNCIA_CIDADE_CHAVE`, `IX_USUARIO_CIDADE_CHAVE`). O Oracle só usa o
  índice se a expressão da consulta for idêntica; um teste de unidade confere isso. Se o Oracle recusar o índice,
  o script só registra o aviso e a API funciona igual, sem ele.
- **Moderação de comentários.**
  - Filtro simples de termos ofensivos (`service.moderacao.FiltroTermosOfensivos`): ao criar ou editar um
    comentário com palavrão ou ofensa comum, a API responde 400 no campo `texto`. Compara palavra inteira, sem
    acentos e sem diferenciar maiúsculas. Não é moderação completa: grafias disfarçadas passam.
  - Reporte pela comunidade: `POST /comentarios/{id}/reporte` (🔒, 204). Cada pessoa reporta um comentário uma
    vez (chave primária em `TB_REPORTE_COMENTARIO`); reportar o próprio comentário dá 422. Com **3 reportes** o
    comentário some da conversa para todos e deixa de contar no total de comentários da denúncia e nos relatórios.
    Ele continua no banco: a contagem é uma subconsulta em `TB_REPORTE_COMENTARIO`, sem coluna de status.
  - Ao excluir comentário, denúncia ou conta, os reportes ligados são apagados antes, na mesma transação.
- **"Cidade viva".** Na primeira conta de uma cidade, a API cria moradores e denúncias de exemplo (marcados como `simulado`), para que o avaliador veja votos, comentários e resolução funcionando. Esses moradores não fazem login, não aparecem nas listas de usuários e podem ser removidos com `DELETE /api/simulacao`.

## 6. Limitações conhecidas

- O token de sessão não pode ser revogado antes de vencer (7 dias): sair no front apaga o token do navegador, mas uma cópia continuaria válida. A exclusão da conta invalida o token na hora (a API confere se o usuário existe). O próximo passo seria uma lista de sessões no banco.
- O limite de tentativas de login fica em memória: zera ao reiniciar a API e não seria compartilhado entre várias instâncias.
- Não há perfis de usuário: a administração do catálogo de direitos usa uma chave fixa (`X-Chave-Admin`).
- O pool de conexões é simples: tamanho fixo, sem métricas nem tempo máximo de vida da conexão. Uma conexão
  esquecida aberta (sem `close()`) ocupa a vaga para sempre; por isso todo acesso passa por `Transacao`.
- Comentário oculto por reportes não tem revisão humana nem recurso: só volta se os reportes forem apagados no banco. Enquanto oculto, não pode ser editado nem reportado de novo (404); o autor ainda pode excluí-lo.
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
