# Guia para agentes de IA

Este arquivo é para assistentes de código (Claude Code, Copilot, Cursor, Codex etc.) e para quem chega ao projeto pela primeira vez. Ele resume o que não dá para deduzir lendo só o código. Para o contexto do produto, leia o [README](README.md); para o contrato da API, o [`docs/api.md`](docs/api.md).

## O projeto em uma frase

Monorepo de um trabalho da FIAP (Global Solution 2026/2): uma API REST em **Java 17 puro com JAX-RS** sobre **Oracle**, e um app **React** de celular que abre direto do disco (`file://`). Cidadãos registram problemas do bairro (denúncias) e recebem orientação sobre direitos sociais a partir de um questionário opcional.

## Mapa do repositório

| Caminho | O que é |
|---|---|
| `backend/` | API REST. Código em `src/main/java/br/com/fiap/meusdireitos/`, testes em `src/test/java/...` |
| `backend/db.properties` | Credenciais do Oracle e segredos. **Fora do git. Nunca leia em voz alta, copie ou imprima valores daqui.** Modelo: `db.properties.example` |
| `backend/src/main/resources/simulacao.json` | Catálogo do conteúdo de demonstração ("cidade viva") |
| `frontend/` | App React 19 + Vite 8. Código em `src/`, fotos de demonstração em `public/img/demo/` |
| `frontend/dist/` | **Build versionado de propósito** (a entrega da FIAP exige o `index.html` abrindo direto no navegador) |
| `database/script.sql` | DDL e carga inicial. Só cria o que falta; nunca apaga nem altera |
| `docs/api.md` | Contrato da API (fonte da verdade para o front) com histórico de mudanças na §13 |
| `docs/diagramas/` | Diagramas de classes (Mermaid + SVG), ER e do front |
| `docs/insomnia/` | Coleção do Insomnia com todas as rotas e uma pasta de erros |
| `testes/api-ponta-a-ponta.mjs` | Teste de ponta a ponta da API (Node 22, sem dependências) |
| `.github/workflows/ci.yml` | CI: `mvn test`, build do front e conferência de que a `dist` versionada está atualizada |

## Comandos

```bash
# Back-end (pasta backend/)
mvn clean compile exec:java          # sobe a API em http://localhost:8080/api (precisa do db.properties)
mvn test                             # testes de unidade, sem banco
MD_INTEGRACAO=true mvn test -Dtest=DaosIntegracaoTest   # integração com o Oracle (ROLLBACK no fim)

# Front-end (pasta frontend/)
npm install
npm run dev                          # http://localhost:5173, usa a API em VITE_API_URL ou localhost:8080
npm run build                        # gera frontend/dist (versionada: faça commit dela depois de mudar o front)

# Ponta a ponta (raiz, com a API rodando)
node testes/api-ponta-a-ponta.mjs [http://127.0.0.1:8080/api]
```

## Regras que não podem ser quebradas

1. **Nenhuma dependência nova.** A FIAP só pontua as tecnologias da disciplina.
   - Back-end: só JDK 17, Jersey (JAX-RS), Grizzly, JSON-B, ojdbc8 e JUnit 5. Nada de Spring, JPA/Hibernate, Lombok, Bean Validation, JWT ou biblioteca de pool (o token e o pool são feitos com o JDK).
   - Front-end: só React, React Router e Bootstrap (apenas o reset e os utilitários). Requisições com `fetch`, nunca axios; nada de Tailwind ou biblioteca de componentes.
2. **O banco Oracle é compartilhado com outro projeto.** O schema tem tabelas que **não são nossas** e não podem ser lidas, alteradas nem citadas: `TB_CHAT_MENSAGEM`, `TB_ITEM_ROTEIRO`, `TB_LISTA_ROTEIRO`, `TB_PONTO_MAPA`, `TB_PRODUTO`, `TB_PRODUTO_ATRIBUTO`, `TB_REGISTRO_RUPTURA`, `TB_SESSAO`, `TODOS`, `USERS` e as sequences `ISEQ$$_*`.
   - As nossas são: `TB_USUARIO`, `TB_PERFIL_DEMOGRAFICO`, `TB_DENUNCIA`, `TB_COMENTARIO`, `TB_VOTO_DENUNCIA`, `TB_CONFIRMACAO_RESOLUCAO`, `TB_REPORTE_COMENTARIO`, `TB_DIREITO`, `TB_DIREITO_PASSO`, `TB_DIREITO_TAG`, `TB_SEMEADURA_CIDADE` e as sequences `SEQ_USUARIO`, `SEQ_DENUNCIA`, `SEQ_COMENTARIO`, `SEQ_DIREITO`.
   - Mudança de estrutura entra **só** no `database/script.sql`, num bloco PL/SQL que verifica antes (`user_tables`, `user_indexes`…) e cria apenas se não existir. Nunca `DROP`, `TRUNCATE` ou `ALTER` destrutivo. Não execute DDL nem DML fora do script sem a pessoa pedir.
3. **Segredos.** Nunca coloque senha, `token.segredo`, chaves de administração/simulação ou tokens de serviços externos em código, documentação, commits ou mensagens. O `frontend/.env.local` (se existir) também é privado.
4. **Contrato da API.** Toda mudança de rota, campo ou código de erro atualiza junto: `docs/api.md` (incluindo uma linha na §13), a coleção do Insomnia e, se afetar o front, `frontend/src`.
5. **Commits pequenos, um por subtarefa**, no formato `tipo(escopo): descrição` em português, minúsculas e sem acento (veja [`CONTRIBUTING.md`](CONTRIBUTING.md)). Sem branch nova e sem push, salvo pedido.

## Arquitetura em 30 segundos

**Back-end** (camadas, sem framework de injeção):

```
resource/ (JAX-RS: rotas, cabeçalhos, status)  →  service/ (regras, validação, transação)
   →  dao/ (interfaces) + dao/jdbc/ (SQL com PreparedStatement)  →  factory/ConnectionFactory (pool)  →  Oracle
```

- `Transacao.executar/consultar(con -> ...)` abre a conexão, faz COMMIT ou ROLLBACK e fecha. Services usam isso; DAOs recebem a `Connection` e nunca fazem COMMIT.
- Erros: services lançam subclasses de `exception/MeusDireitosException` (400, 401, 403, 404, 409, 422, 429, 503); `exception/mapper/` converte tudo no JSON padrão `{status, erro, mensagem, campos?, caminho, timestamp}`. Erros de SQL viram `PersistenciaException` (ex.: ORA-00001 → 409, ORA-12899 → 400 no campo).
- Validação: `service/validacao/Validador` acumula erros por campo e lança tudo de uma vez.
- Sessão: `Authorization: Bearer <token>`, token HMAC-SHA256 de 7 dias (`util/TokenUtil`). As resources leem o cabeçalho pela constante `UsuarioResource.CABECALHO_USUARIO` e chamam `AutenticacaoService.idDoUsuarioLogado(...)` (obrigatório) ou `idOpcional(...)` (rotas públicas que mostram algo a mais a quem está logado).

**Front-end:**

```
main.jsx (HashRouter, SessaoProvider) → App.jsx (rotas) → pages/<seção>/*.jsx
   → components/ (Tela, Cabecalho, Campo, CardDenuncia…) · utils/ (validação, formatos, foco)
   → services/api.js (fetch + token + ErroApi) → API
```

## Armadilhas conhecidas

- **Cidade.** As consultas comparam a cidade com `JdbcUtil.cidadeIgual(coluna)`, que gera `TRANSLATE(UPPER(TRIM(col)), '<acentos>', '<sem acento>') = ?`, e o valor vem de `TextoUtil.chaveCidade`. Os índices `IX_DENUNCIA_CIDADE_CHAVE` e `IX_USUARIO_CIDADE_CHAVE` repetem **exatamente** essa expressão; se ela mudar, mude o script (o teste `IndiceCidadeScriptTest` confere). Não use `REGEXP_REPLACE` nela: o Oracle recusa indexar (ORA-01743). Por isso a cidade é gravada já com `TextoUtil.normalizarEspacos`.
- **Exclusões sem `ON DELETE CASCADE`.** Ao apagar usuário, denúncia ou comentário, o service apaga antes as tabelas filhas (votos, comentários, reportes, confirmações) na mesma transação. Tabela nova com FK precisa entrar nessas sequências (`UsuarioService.excluirConta`, `DenunciaService.excluir`, `ComentarioService.excluir`, `SimulacaoService.limparTudo`).
- **`file://`.** O front usa `HashRouter`, `base: './'` e um build em IIFE (plugin `abrirDiretoNoNavegador` no `vite.config.js`) para abrir sem servidor. Nesse caso o navegador manda a origem `null`, liberada por padrão em `cors.origens` (quem trocar a lista precisa mantê-la).
- **Sessão no front.** `services/sessao.js` copia token e usuário para a memória quando o app carrega. Testes automatizados que gravam no `localStorage` precisam recarregar a página (trocar só o `#` não recarrega).
- **Windows e `localhost`.** A API escuta em IPv4 e IPv6 (`Main`) para clientes que resolvem `localhost` como `::1` não esperarem ~2 s.
- **Conteúdo de demonstração.** A primeira pessoa de uma cidade dispara a "cidade viva" (moradores e denúncias de exemplo, marcados com `st_simulado = 'S'`). Contas reais e simuladas convivem nas mesmas tabelas; consultas públicas escondem os moradores de exemplo.
- **Finais de linha.** O `.gitattributes` normaliza para LF. No Windows os arquivos de trabalho podem estar em CRLF; preserve o que o arquivo já usa ao editar.
- **`frontend/dist/`** fica desatualizada se você mudar o front e não rodar `npm run build`. Faça o commit do build em separado (`build(frontend): ...`).

## Onde mexer para…

| Quero… | Arquivos |
|---|---|
| Criar uma rota da API | `resource/*Resource.java` → `service/*Service.java` → `dao/*Dao.java` + `dao/jdbc/*DaoJdbc.java`; DTOs em `dto/request` e `dto/response`; documente em `docs/api.md` e no Insomnia |
| Mudar uma regra de validação | `service/validacao/Validador.java` (back) e `frontend/src/utils/validacao.js` (front, mesmas regras) |
| Adicionar uma tabela | `database/script.sql` (bloco "só cria se não existir"), `docs/diagramas/diagrama-er.md`, exclusões em cascata nos services |
| Criar uma tela | `frontend/src/pages/<seção>/`, rota em `App.jsx`, estilos com as classes `md-*` de `styles/tema.css` (ver o [README do front](frontend/README.md)) |
| Mudar cores, tipografia ou movimento | `frontend/src/styles/tema.css` (tokens `--md-*`) |
| Mudar o conteúdo de demonstração | `backend/src/main/resources/simulacao.json` e `service/simulacao/` |
| Mudar o catálogo de direitos | `database/script.sql` (carga inicial) ou as rotas administrativas `/direitos` com `X-Chave-Admin` |

## Como verificar uma mudança

1. `mvn test` no `backend/` (deve continuar verde; acrescente testes de unidade para regra nova).
2. `npm run build` no `frontend/` (o build não pode quebrar).
3. Com a API rodando contra o Oracle: `node testes/api-ponta-a-ponta.mjs`. Ele cria contas de teste e as exclui no fim.
4. Para telas, abra o app em 390 px de largura (é um app de celular) e confira foco, leitura por leitor de tela e `prefers-reduced-motion`.

## Estilo de código e textos

- Código, nomes, comentários, mensagens de erro e documentação em **português do Brasil**. Nomes de classes e métodos em português (`DenunciaService.alterarStatus`), sem abreviações obscuras.
- Comentários explicam o **porquê** (decisões, armadilhas), não o que a linha faz.
- Mensagens para o usuário: frases curtas, sem jargão técnico, dizendo o que fazer (ex.: "Faça login para continuar.").
- Front: acessibilidade é requisito (rótulos, `aria-*`, alvos de 44 px, foco visível, hover só em `(hover: hover) and (pointer: fine)`).
