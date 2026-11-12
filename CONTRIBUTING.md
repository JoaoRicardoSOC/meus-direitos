# Como contribuir

Guia para os integrantes do grupo (e para quem continuar o projeto) trabalharem do mesmo jeito. Agentes de IA seguem também o [`AGENTS.md`](AGENTS.md).

## 1. Preparar o ambiente

| Ferramenta | Versão | Para quê |
|---|---|---|
| JDK | 17 | Back-end |
| Maven | 3.9 ou mais nova | Compilar, testar e subir a API |
| Node.js | 22 | Front-end e teste de ponta a ponta |
| Oracle SQL Developer | qualquer | Rodar o `database/script.sql` (opcional: a API também roda com o schema já criado) |
| Insomnia | qualquer | Testar a API à mão com a coleção de `docs/insomnia/` |

1. Copie `backend/db.properties.example` para `backend/db.properties` e preencha usuário, senha e um `token.segredo` aleatório (as instruções estão no próprio arquivo). Esse arquivo não vai para o git.
2. Suba a API: `mvn clean compile exec:java` na pasta `backend/`.
3. Suba o front: `npm install` e `npm run dev` na pasta `frontend/`.

## 2. Antes de mudar algo

- Leia a parte do projeto que vai mexer: [`backend/README.md`](backend/README.md), [`frontend/README.md`](frontend/README.md) e [`docs/api.md`](docs/api.md).
- **Banco compartilhado:** o schema do Oracle tem tabelas de outro projeto. Só mexa nas tabelas deste projeto, e só pelo `database/script.sql` (a lista completa está no [`AGENTS.md`](AGENTS.md#regras-que-não-podem-ser-quebradas)).
- **Sem dependências novas** no `pom.xml` nem no `package.json`: a FIAP só pontua as tecnologias da disciplina.

## 3. Commits

Um commit para cada mudança lógica. Muitos commits pequenos são melhores que poucos grandes: fica fácil revisar e desfazer.

**Formato:** `tipo(escopo): descrição curta`, em português, com letras minúsculas e sem acento.

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feat` | Funcionalidade nova | `feat(cidade): busca e filtros no feed` |
| `fix` | Correção de erro | `fix(perfil): foca o primeiro erro na troca de senha` |
| `perf` | Desempenho | `perf(db): indexa a comparacao de cidade` |
| `refactor` | Reorganização sem mudar o comportamento | `refactor(frontend): move a localizacao para services` |
| `test` | Testes | `test(backend): testes de integracao dos DAOs` |
| `docs` | Documentação | `docs(api): documenta o reporte de comentario` |
| `build` | Build, dependências, `dist/` | `build(frontend): atualiza a dist` |
| `ci` | GitHub Actions | `ci: roda os testes a cada push` |
| `chore` | Manutenção que não entra nas outras | `chore: atualiza o gitignore` |

Escopos usados: `backend`, `frontend`, `db`/`banco`, `api`, `readme`, `diagramas`, `insomnia` e as seções do app (`cidade`, `denuncias`, `direitos`, `perfil`, `inicio`).

Se um arquivo tiver duas mudanças diferentes, separe com `git add -p`.

## 4. Checklist de um pull request

- [ ] `mvn test` passa no `backend/`.
- [ ] `npm run build` passa no `frontend/`; se o front mudou, a `frontend/dist/` foi gerada de novo e está num commit `build(frontend): ...`.
- [ ] Mudou a API? `docs/api.md` (com uma linha no histórico, §13) e a coleção do Insomnia estão atualizados.
- [ ] Mudou o banco? O bloco novo no `database/script.sql` só cria o que não existe, e o `docs/diagramas/diagrama-er.md` foi atualizado.
- [ ] Com a API rodando: `node testes/api-ponta-a-ponta.mjs` passa.
- [ ] Telas novas funcionam em 390 px, com teclado e leitor de tela, e respeitam `prefers-reduced-motion`.
- [ ] Nenhuma senha, segredo ou token no diff.

A CI do GitHub ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) confere os dois primeiros itens a cada push.

## 5. Testes

| Tipo | Onde | Como rodar | Precisa do banco? |
|---|---|---|---|
| Unidade (back-end) | `backend/src/test/java/` | `mvn test` | Não |
| Integração dos DAOs | `backend/src/test/java/.../integracao/` | `MD_INTEGRACAO=true mvn test -Dtest=DaosIntegracaoTest` | Sim. Tudo é desfeito com ROLLBACK |
| Ponta a ponta da API | `testes/api-ponta-a-ponta.mjs` | `node testes/api-ponta-a-ponta.mjs` | Sim. As contas de teste são excluídas no fim |
| Manual | `docs/insomnia/` | Importe a coleção no Insomnia | Sim |

## 6. Estilo

- Tudo em português do Brasil: nomes no código, comentários, mensagens e documentação.
- Comentários explicam o porquê, não o que a linha faz.
- Mensagens para o usuário: curtas, sem termos técnicos, dizendo o que fazer.
- Front-end: use os tokens `--md-*` e as classes `md-*` de `frontend/src/styles/tema.css` em vez de cores e medidas soltas.
