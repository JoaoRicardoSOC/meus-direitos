# Guia de integração — API Meus Direitos

> **Para:** front-end (React + Vite). **Mantido por:** equipe de back-end.
> **Última atualização:** 12/11/2026. Este guia acompanha cada entrega do backend; consulte a seção [Histórico](#13-histórico-de-mudanças) para ver o que mudou.

## Sumário

1. [O que já está pronto](#1-o-que-já-está-pronto)
2. [Rodando a API na sua máquina](#2-rodando-a-api-na-sua-máquina)
3. [Convenções](#3-convenções)
4. [Sessão do usuário (login)](#4-sessão-do-usuário-login)
5. [Cliente `fetch` recomendado](#5-cliente-fetch-recomendado)
6. [Erros](#6-erros)
7. [Mapa: telas do Figma → chamadas](#7-mapa-telas-do-figma--chamadas)
8. [Referência dos endpoints](#8-referência-dos-endpoints) — inclui [relatórios](#89-relatórios-da-cidade)
9. [Objetos devolvidos pela API](#9-objetos-devolvidos-pela-api)
10. [Dicas de interface](#10-dicas-de-interface)
11. [Pedidos do back-end para o front-end](#11-pedidos-do-back-end-para-o-front-end)
12. [Regras da FIAP que afetam o front](#12-regras-da-fiap-que-afetam-o-front)
13. [Histórico de mudanças](#13-histórico-de-mudanças)

---

## 1. O que já está pronto

| Área | Status |
|---|---|
| Saúde da API e listas para `<select>` (categorias, raça/cor, deficiências, percurso escolar, status) | ✅ Pronto |
| Cadastro, login, conta do usuário | ✅ Pronto |
| Perfil "Meus Direitos" (questionário) e temas de direitos aplicáveis | ✅ Pronto |
| Catálogo de direitos (detalhe com passos "Por onde começar") | ✅ Pronto |
| Denúncias: criar, ver, editar, excluir, "minhas", busca pública | ✅ Pronto |
| Feed da cidade: **Para você**, **Recentes**, **Mais votados** | ✅ Pronto |
| Votos (↑ / ↓) com placar atualizado | ✅ Pronto |
| Conversa do bairro (comentários) | ✅ Pronto |
| Resolver denúncia: autor marca **ou** 3 moradores confirmam | ✅ Pronto |
| Relatórios: "Números da cidade", por categoria, por bairro, mais engajadas | ✅ Pronto |
| Trocar senha / excluir conta | ✅ Pronto |
| "Cidade viva": conteúdo de exemplo automático em cidades novas, atividade simulada, selo "Exemplo" | ✅ Pronto |

---

## 2. Rodando a API na sua máquina

**Pré-requisitos:** JDK 17 ou superior e Maven 3.9+.

1. Peça a um integrante do back-end o arquivo **`db.properties`** (tem as credenciais do Oracle) e salve em `backend/db.properties`. Esse arquivo é ignorado pelo git — **nunca faça commit dele**. O modelo está em [`backend/db.properties.example`](../backend/db.properties.example).
2. Na pasta `backend`:

   ```bash
   mvn clean compile exec:java
   ```

3. Abra <http://localhost:8080/api/health>. Deve aparecer `{"banco":"UP","status":"UP",...}`.
   - `"banco":"INDISPONIVEL"` → a API subiu, mas não conectou no Oracle (credenciais ou rede da FIAP).

**CORS:** a API aceita chamadas do navegador vindas de `http://localhost:5173` (porta padrão do Vite). Se o seu Vite rodar em outra porta, adicione-a no `db.properties`:

```properties
cors.origens=http://localhost:5173,http://localhost:5174
```

---

## 3. Convenções

| Item | Como funciona |
|---|---|
| **Base URL** | `http://localhost:8080/api` |
| **Formato** | JSON em UTF-8 (`Content-Type: application/json`) |
| **Nomes** | `camelCase` |
| **Datas** | ISO-8601: data `"1958-03-12"`; data e hora `"2026-11-11T17:13:00"` (horário local, sem fuso) |
| **Valores de lista (enums)** | Enviados como código em MAIÚSCULAS: `"CALCADA_ACESSIBILIDADE"`. Nas respostas vêm como `{"codigo": "...", "descricao": "Calçada e acessibilidade"}` — **mostre a `descricao`, envie o `codigo`**. |
| **Campos nulos** | ⚠️ **Não aparecem no JSON.** Ex.: `placar.meuVoto` só existe se a pessoa votou. Sempre trate a ausência: `denuncia.placar.meuVoto ?? null`. |
| **IDs** | Números inteiros |
| **Rotas com 🔒** | Exigem o cabeçalho `X-Usuario-Id` (ver seção 4) |

---

## 4. Sessão do usuário (login)

A API não usa token: depois do cadastro ou do login, ela devolve o `id` do usuário, e o front envia esse id em **toda** chamada que precisa de login.

```
1. POST /api/usuarios  (cadastro)  ou  POST /api/auth/login
2. resposta.id  →  localStorage.setItem('usuarioId', resposta.id)
3. Toda chamada 🔒  →  cabeçalho  X-Usuario-Id: <id>
4. "Sair da conta"  →  localStorage.removeItem('usuarioId')  (não existe rota de logout)
5. Resposta 401 em qualquer rota  →  apague o id e mande para a tela "Entrar"
```

Guarde também o objeto do usuário (nome, cidade) se quiser exibi-lo sem chamar `/usuarios/me` toda hora.

---

## 5. Cliente `fetch` recomendado

Copie para algo como `src/services/api.js`. Usa apenas `fetch` (padrão do navegador).

```js
const API_URL = 'http://localhost:8080/api';

/** Erro devolvido pela API, já com status, mensagem e erros por campo. */
export class ErroApi extends Error {
  constructor(corpo) {
    super(corpo.mensagem);
    this.status = corpo.status;
    this.campos = corpo.campos ?? [];       // [{ campo: 'email', mensagem: '...' }]
  }
}

export async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const cabecalhos = { 'Content-Type': 'application/json' };
  const usuarioId = localStorage.getItem('usuarioId');
  if (usuarioId) cabecalhos['X-Usuario-Id'] = usuarioId;

  let resposta;
  try {
    resposta = await fetch(API_URL + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi({ status: 0, mensagem: 'Não foi possível conectar ao servidor.' });
  }

  if (resposta.status === 204) return null;               // sucesso sem corpo (DELETE)
  const dados = await resposta.json();
  if (!resposta.ok) {
    if (resposta.status === 401) localStorage.removeItem('usuarioId');
    throw new ErroApi(dados);
  }
  return dados;
}
```

**Exemplo em um componente (loading + erro):**

```jsx
const [denuncias, setDenuncias] = useState([]);
const [carregando, setCarregando] = useState(true);
const [erro, setErro] = useState(null);

useEffect(() => {
  api('/denuncias/feed?ordem=PARA_VOCE')
    .then((pagina) => setDenuncias(pagina.itens))
    .catch((e) => setErro(e.message))
    .finally(() => setCarregando(false));
}, []);
```

**Exemplo em formulário (erros ao lado de cada campo):**

```jsx
try {
  await api('/usuarios', { metodo: 'POST', corpo: dadosDoFormulario });
} catch (e) {
  // e.campos = [{ campo: 'email', mensagem: 'E-mail em formato inválido.' }, ...]
  const porCampo = Object.fromEntries(e.campos.map((c) => [c.campo, c.mensagem]));
  setErrosDosCampos(porCampo);   // mostre porCampo.email embaixo do input de e-mail
  setMensagemGeral(e.message);   // ex.: "Verifique os campos destacados."
}
```

> A API valida **tudo de uma vez** e devolve todos os campos com problema na mesma resposta.

---

## 6. Erros

Todo erro vem neste formato:

```json
{
  "status": 400,
  "erro": "Dados inválidos",
  "mensagem": "Verifique os campos destacados.",
  "campos": [
    { "campo": "nome", "mensagem": "Deve ter pelo menos 3 caracteres." },
    { "campo": "email", "mensagem": "E-mail em formato inválido." }
  ],
  "caminho": "/api/usuarios",
  "timestamp": "2026-11-11T17:13:08.66"
}
```

| Status | Quando acontece | O que o front faz |
|---|---|---|
| 400 | Campo inválido, JSON mal formado, valor de lista inexistente | Mostra `mensagem` e cada item de `campos` ao lado do input |
| 401 | Sem login, `X-Usuario-Id` inválido, e-mail/senha incorretos | Volta para "Entrar" (no login, mostra a mensagem) |
| 403 | Ação de outra pessoa (ex.: editar denúncia alheia) | Mostra `mensagem` |
| 404 | Recurso não existe; **em `GET /usuarios/perfil` significa "ainda não respondeu o questionário"** | Tela de vazio / questionário |
| 405 | Método HTTP errado para a rota (ex.: `DELETE` onde só existe `GET`) | Erro de programação: confira a seção 8 |
| 409 | E-mail já cadastrado, título repetido | Mostra `mensagem` no campo correspondente |
| 415 | Corpo enviado sem `Content-Type: application/json` | Erro de programação: o cliente da seção 5 já envia o cabeçalho |
| 422 | Regra do negócio (votar na própria denúncia, mudança de status inválida) | Mostra `mensagem` (toast/alerta) |
| 503 | Banco de dados fora do ar | "Serviço indisponível, tente novamente" |
| 500 | Erro inesperado | Mensagem genérica |

---

## 7. Mapa: telas do Figma → chamadas

| Tela (Figma) | Chamadas |
|---|---|
| 01 Boas-vindas | — |
| 02–05 Cadastro (nome, cidade, e-mail, senha) | guarde as respostas no estado e, no fim, `POST /usuarios` |
| 06 Entrar | `POST /auth/login` |
| 07 / 33 Feed "Nossa cidade" | `GET /denuncias/feed?ordem=...` (abas) |
| 08 / 09 Voto registrado / contrário | `PUT /denuncias/{id}/voto` · clicar de novo no mesmo voto: `DELETE /denuncias/{id}/voto` |
| 10 Detalhes da denúncia | `GET /denuncias/{id}` + `GET /denuncias/{id}/comentarios` |
| 11 Comentário enviado | `POST /denuncias/{id}/comentarios` |
| 12 / 13 Nova denúncia | `GET /categorias` (select) → `POST /denuncias` |
| 14 Categorias | `GET /categorias` |
| 15 Foto / prévia | só front (envie o caminho da imagem em `imagemUrl`) |
| 16 Denúncia publicada | use `protocolo` da resposta do `POST /denuncias` |
| 17 Minha denúncia | `GET /denuncias/minhas` · Editar: `PUT /denuncias/{id}` · Excluir: `DELETE /denuncias/{id}` · **Marcar como resolvida: `PATCH /denuncias/{id}/status`** |
| 18 Excluir denúncia | `DELETE /denuncias/{id}` |
| 19 Perfil | `GET /usuarios/me` |
| 20 / 21 Editar perfil | `PUT /usuarios/me` |
| *(sem tela)* Trocar senha · Excluir conta | `PUT /usuarios/me/senha` · `POST /usuarios/me/exclusao` |
| *(sem tela)* Números da cidade | `GET /relatorios/resumo` (sugestão: bloco no topo do feed) |
| 22 Meus Direitos (primeiro acesso) | `GET /usuarios/perfil` → **404** = mostrar esta tela |
| 23–27 Questionário | `GET /racas-cor`, `/deficiencias`, `/percursos-escolares` (selects) → no fim, `PUT /usuarios/perfil` |
| 28 Meus Direitos (temas) | `GET /direitos/meus` (ou use `direitos` da resposta do `PUT /usuarios/perfil`) |
| 29–31 Detalhe do tema | `GET /direitos/{id}` |
| 32 Canais oficiais | estático no front (Prefeitura, Gov.br, CRAS) + `linkOficial` do tema |

---

## 8. Referência dos endpoints

### 8.1 Infra e listas para `<select>`

| Método | Rota | Resposta |
|---|---|---|
| GET | `/health` | `{"status":"UP","banco":"UP","dataHora":"..."}` |
| GET | `/categorias` | `[{"codigo":"ASFALTO","descricao":"Asfalto"}, ...]` |
| GET | `/status-denuncia` | `ABERTA` (Em aberto), `EM_ANDAMENTO`, `RESOLVIDA` |
| GET | `/racas-cor` | `BRANCA, PRETA, PARDA, AMARELA, INDIGENA, NAO_INFORMADO` |
| GET | `/deficiencias` | `NENHUMA, FISICA, VISUAL, AUDITIVA, INTELECTUAL, NAO_INFORMADO` |
| GET | `/percursos-escolares` | `ESCOLA_PUBLICA, ESCOLA_PARTICULAR, MISTO, NAO_INFORMADO` |

Categorias de denúncia: `ASFALTO` (Asfalto), `ILUMINACAO` (Iluminação), `CALCADA_ACESSIBILIDADE` (Calçada e acessibilidade), `SEGURANCA` (Segurança no bairro), `LIMPEZA_URBANA` (Limpeza urbana), `OUTROS` (Outros).

### 8.2 Usuários e login

#### `POST /usuarios` — cadastro

```json
{ "nome": "Mariana Oliveira", "email": "mariana@exemplo.com", "senha": "senha1234", "cidade": "São Paulo", "uf": "SP" }
```

Resposta **201** (`UsuarioResponse`):

```json
{ "id": 7, "nome": "Mariana Oliveira", "email": "mariana@exemplo.com", "cidade": "São Paulo", "uf": "SP",
  "criadoEm": "2026-11-11T00:25:11", "perfilPreenchido": false }
```

Regras: nome 3–100; e-mail válido (até 120, guardado em minúsculas); **senha 8–64 com pelo menos uma letra e um número**; cidade 2–80; `uf` = sigla de estado válida. E-mail já usado → **409**.

> ⏳ **Cidade nova demora um pouco mais (cerca de 1 a 2 s no Oracle da FIAP).** Se a pessoa é a primeira da cidade, a API prepara o conteúdo de exemplo da cidade ("cidade viva", seção 8.10) antes de responder. Mostre **"Preparando sua cidade…"** depois de enviar o cadastro. A resposta traz `"cidadePreparada": true`; se vier `false`, o cadastro funcionou, mas o feed terá só conteúdo real (sugestão: "Sua cidade ainda está começando").

> A tela 03 tem um campo único "Cidade e estado". A API precisa de **`cidade` e `uf` separados**. Recomendado: um `<select>` de cidades (ver seção 11).

#### `POST /auth/login`

```json
{ "email": "mariana@exemplo.com", "senha": "senha1234" }
```

**200** → `UsuarioResponse` (o mesmo do cadastro). E-mail ou senha errados → **401** `"E-mail ou senha inválidos."`

| Método | Rota | Descrição |
|---|---|---|
| GET 🔒 | `/usuarios/me` | Conta do usuário logado (`UsuarioResponse`) |
| PUT 🔒 | `/usuarios/me` | `{ "nome", "email", "cidade", "uf" }` → `UsuarioResponse`. E-mail de outra conta → 409. Se a cidade mudou, a API prepara a "cidade viva" da cidade nova (1 a 2 s a mais) e a resposta traz `cidadePreparada` |
| GET | `/usuarios` | Lista pública `[{ "id", "nome", "cidade", "uf" }]` |
| GET | `/usuarios/{id}` | `{ "id", "nome", "cidade", "uf", "totalDenuncias" }`. Morador de exemplo → 404 |

`perfilPreenchido` indica se o usuário já respondeu o questionário de "Meus Direitos".

#### `PUT /usuarios/me/senha` 🔒 — trocar senha

```json
{ "senhaAtual": "senha1234", "novaSenha": "novaSenha2026" }
```

**204** se deu certo. Erros (todos **400**, com o campo indicado em `campos`):

| Campo | Mensagem |
|---|---|
| `senhaAtual` | "Senha atual incorreta." |
| `novaSenha` | mesma regra do cadastro (8–64, letra e número) ou "A nova senha deve ser diferente da atual." |

> A senha atual errada vem como **400 no campo**, não 401 — então o cliente da seção 5 **não** desloga a pessoa por um erro de digitação.

#### `POST /usuarios/me/exclusao` 🔒 — excluir a conta

```json
{ "senha": "senha1234" }
```

- **204** → conta excluída. Apague o `usuarioId` do `localStorage` e volte para a tela inicial.
- Senha errada → **400** no campo `senha` ("Senha incorreta.").
- **O que é apagado** (não tem volta): a conta, o questionário, as denúncias da pessoa e, junto com elas, votos, comentários e confirmações que estavam nelas, além dos votos, comentários e confirmações que a pessoa fez em denúncias de outros.
- É `POST` (e não `DELETE`) porque a senha vai no corpo.
- Sugestão de tela: confirmação "Excluir minha conta" com campo de senha (não há tela no Figma).

### 8.3 Perfil "Meus Direitos" (questionário)

#### `PUT /usuarios/perfil` 🔒 — salvar respostas

**Todos os campos são opcionais** ("Pular por enquanto" = não enviar ou enviar `null`; "Prefiro não informar" = `NAO_INFORMADO`).

```json
{ "dataNascimento": "1958-03-12", "racaCor": "NAO_INFORMADO", "deficiencia": "FISICA",
  "percursoEscolar": "ESCOLA_PUBLICA", "baixaRenda": true }
```

Resposta **200** (`PerfilCompletoResponse`):

```json
{
  "perfil": { "dataNascimento": "1958-03-12", "idade": 68,
              "racaCor": { "codigo": "NAO_INFORMADO", "descricao": "Prefiro não informar" },
              "deficiencia": { "codigo": "FISICA", "descricao": "Física" },
              "percursoEscolar": { "codigo": "ESCOLA_PUBLICA", "descricao": "Escola pública" },
              "baixaRenda": true, "atualizadoEm": "2026-11-11T00:37:33" },
  "tags": [ { "codigo": "IDOSO", "descricao": "Pessoa idosa (60+)" },
            { "codigo": "PCD", "descricao": "Pessoa com deficiência" } ],
  "direitos": [ { "id": 1, "titulo": "Transporte e mobilidade",
                  "resumo": "Saiba onde consultar gratuidades e programas de transporte.", "icone": "TRANSPORTE" } ]
}
```

- `dataNascimento` precisa estar no passado (até 120 anos).
- `baixaRenda` corresponde à pergunta opcional "Sua família está no CadÚnico?" (ver seção 11).
- `direitos` já vem pronto para a tela 28 — não precisa chamar outra rota.

| Método | Rota | Descrição |
|---|---|---|
| GET 🔒 | `/usuarios/perfil` | Mesmo corpo do PUT. **404 = ainda não respondeu** → mostrar tela 22 |
| DELETE 🔒 | `/usuarios/perfil` | Apaga as respostas (dados sensíveis) → 204 |

### 8.4 Direitos

| Método | Rota | Descrição |
|---|---|---|
| GET 🔒 | `/direitos/meus` | Cards dos temas aplicáveis ao usuário (`DireitoResumoResponse[]`). Sem questionário → `[]` |
| GET | `/direitos` | Catálogo completo. Filtro opcional: `?tag=IDOSO` |
| GET | `/direitos/{id}` | Tema completo (`DireitoResponse`, telas 29–31) |
| POST / PUT / DELETE | `/direitos`, `/direitos/{id}` | Cadastro do catálogo (uso administrativo; o front não precisa). Exige o cabeçalho `X-Chave-Admin`; sem ele → **403** |

`DireitoResponse` (tela de detalhe):

```json
{ "id": 1, "titulo": "Transporte e mobilidade",
  "resumo": "Saiba onde consultar gratuidades e programas de transporte.",
  "descricao": "Passe Livre e outros programas de transporte. Exemplos para pesquisa.",
  "passos": [ { "ordem": 1, "titulo": "Identifique o programa",
                "texto": "Verifique se o deslocamento é municipal, intermunicipal ou interestadual." },
              { "ordem": 2, "titulo": "Consulte o órgão responsável",
                "texto": "Procure o portal oficial do serviço de transporte e confira os critérios atuais." } ],
  "aviso": "Os critérios variam por programa e localidade. Confirme as regras no órgão responsável.",
  "baseLegal": "Lei 10.741/2003 (Estatuto da Pessoa Idosa), arts. 39 e 40; Lei 8.899/1994",
  "linkOficial": "https://www.gov.br/...", "icone": "TRANSPORTE", "ordem": 1,
  "tags": [ { "codigo": "IDOSO", "descricao": "Pessoa idosa (60+)" } ] }
```

Valores de `icone`: `TRANSPORTE`, `PRIORIDADE`, `MUNICIPIO`, `EDUCACAO`, `SAUDE`, `RENDA`, `OUTRO` — mapeie cada um para o ícone do Figma.

### 8.5 Denúncias

#### `POST /denuncias` 🔒 — nova denúncia

```json
{ "titulo": "Buraco próximo à faixa de pedestres",
  "descricao": "O asfalto cedeu perto da faixa e dificulta a travessia de moradores.",
  "categoria": "ASFALTO", "endereco": "Rua das Flores, 120", "bairro": "Centro",
  "imagemUrl": "img/demo/asfalto-1.jpg" }
```

- **Cidade e UF não são enviadas**: a denúncia fica na cidade do usuário.
- Regras: título 5–120; descrição 10–1000; categoria obrigatória; endereço 5–150; bairro até 80 (opcional); `imagemUrl` opcional, até 300: endereço `https://` **ou** caminho do front como `img/demo/asfalto-1.jpg` (recusa `javascript:`, `data:` e `..` → **400** no campo).
- Resposta **201** com `DenunciaResponse` (seção 9). Use **`protocolo`** na tela 16 ("PROTOCOLO DEMO · #2026-0142").

| Método | Rota | Descrição |
|---|---|---|
| GET 🔒 | `/denuncias/feed?ordem=&pagina=&tamanho=` | **Feed da cidade do usuário** (`PaginaResponse`). `ordem`: `PARA_VOCE` (padrão), `RECENTES`, `MAIS_VOTADOS` |
| GET | `/denuncias?cidade=&uf=&categoria=&status=&busca=&pagina=&tamanho=` | Listagem pública com filtros opcionais, da mais recente para a mais antiga. `busca` até 100 caracteres |
| GET 🔒 | `/denuncias/minhas` | Denúncias do usuário (lista simples, sem paginação) |
| GET | `/denuncias/{id}` | Detalhe. Se enviar `X-Usuario-Id`, vem `meuVoto` e `euConfirmei` |
| PUT 🔒 | `/denuncias/{id}` | Mesmo corpo do POST. **Só o autor** (403) |
| PATCH 🔒 | `/denuncias/{id}/status` | `{ "status": "RESOLVIDA" }`. **Só o autor**. Ver 8.7 |
| DELETE 🔒 | `/denuncias/{id}` | **Só o autor**. Apaga votos, comentários e confirmações junto → 204 |

**Paginação:** `pagina` começa em 1; `tamanho` padrão 10, máximo 50. Use `temProxima` para o botão "Carregar mais".

#### O feed e as abas

| Aba | `ordem` | Como ordena |
|---|---|---|
| **Para você** | `PARA_VOCE` | Votos **+ prioridade** para categorias que afetam o perfil do usuário (ex.: calçada para idosos e PCD). Resolvidas vão para o fim. |
| Recentes | `RECENTES` | Mais novas primeiro |
| Mais votados | `MAIS_VOTADOS` | Saldo de votos (↑ − ↓) |

No feed, cada item traz **`relevanteParaMim: true`** quando a categoria afeta o perfil — sugestão: um selo "Para você" no card.

### 8.6 Votos

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| PUT 🔒 | `/denuncias/{id}/voto` | `{ "tipo": "UP" }` ou `{ "tipo": "DOWN" }` | `PlacarResponse` |
| DELETE 🔒 | `/denuncias/{id}/voto` | — | `PlacarResponse` |

```json
{ "upvotes": 15, "downvotes": 2, "saldo": 13, "meuVoto": "UP" }
```

- Repetir o mesmo voto não muda nada; votar no oposto troca o voto.
- **Comportamento sugerido no botão:** se `meuVoto` já é o tipo clicado → `DELETE` (desmarcar); senão → `PUT`.
- **Atualize o card com o placar da resposta** (contagem instantânea, sem recarregar o feed).
- Votar na **própria** denúncia → **422**. Sugestão: esconder/desabilitar os botões quando `denuncia.autor.id === usuarioLogado.id`.

### 8.7 Resolver uma denúncia

Dois caminhos:

1. **O autor marca como resolvida** (tela 17, botão "Marcar como resolvida"):
   `PATCH /denuncias/{id}/status` com `{ "status": "RESOLVIDA" }`.
   Transições aceitas: `ABERTA → EM_ANDAMENTO`, `ABERTA/EM_ANDAMENTO → RESOLVIDA`, `RESOLVIDA → ABERTA` (botão "Reabrir"). Outras → **422**.
2. **A comunidade confirma** (tela 10, botão "Já foi resolvido" para quem **não** é o autor):
   - `PUT /denuncias/{id}/confirmacao` → confirma; `DELETE` → retira a confirmação.
   - Resposta: a `DenunciaResponse` atualizada.
   - **Na 3ª confirmação de moradores diferentes, `status` vira `RESOLVIDA` automaticamente.**

Toda denúncia traz o progresso:

```json
"resolucao": { "confirmacoes": 2, "necessarias": 3, "euConfirmei": true }
```

→ texto sugerido: **"2 de 3 moradores confirmaram que foi resolvido"**.

| Situação | Resposta |
|---|---|
| Autor tenta confirmar | 422 (ele usa "Marcar como resolvida") |
| Morador de **outra cidade** | 403 |
| Denúncia já resolvida (confirmar ou retirar) | 422 |
| Autor reabre a denúncia | as confirmações voltam a 0 |

Quando mostrar o botão "Já foi resolvido": usuário logado, **não** é o autor, mesma cidade da denúncia e `status.codigo !== 'RESOLVIDA'`. Se `euConfirmei` for `true`, o botão vira "Retirar confirmação".

### 8.8 Conversa do bairro (comentários)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/denuncias/{id}/comentarios` | Lista do mais antigo ao mais novo. Com `X-Usuario-Id`, cada item indica `meu` |
| POST 🔒 | `/denuncias/{id}/comentarios` | `{ "texto": "Também notei esse problema." }` (2–500 caracteres) → 201 |
| PUT 🔒 | `/comentarios/{id}` | Editar o próprio comentário (403 se for de outra pessoa) |
| DELETE 🔒 | `/comentarios/{id}` | Excluir o próprio comentário → 204 |

```json
{ "id": 55, "idDenuncia": 1, "texto": "Também notei esse problema.",
  "autor": { "id": 7, "nome": "Mariana Oliveira", "simulado": false }, "meu": true,
  "criadoEm": "2026-11-11T01:29:49", "editadoEm": "2026-11-11T01:35:02" }
```

`editadoEm` só aparece se o comentário foi editado (mostre "editado"). O contador do card vem em `denuncia.comentarios`.

### 8.9 Relatórios da cidade

Rotas **públicas**. A cidade vem de `?cidade=&uf=`; se você **não** enviar, a API usa a cidade do usuário logado (pelo `X-Usuario-Id`). Sem nenhum dos dois → **400**.

| Método | Rota | Para quê |
|---|---|---|
| GET | `/relatorios/resumo` | Bloco **"Números da cidade"** (sugestão: topo do feed ou tela de impacto) |
| GET | `/relatorios/categorias` | Denúncias por categoria e situação (gráfico de barras) |
| GET | `/relatorios/bairros?minimo=1` | Bairros com mais problemas; `minimo` = só bairros com pelo menos N denúncias |
| GET | `/relatorios/engajamento?limite=5` | Denúncias com mais votos + comentários (`limite` 1–20) |

`GET /relatorios/resumo`:

```json
{ "cidade": "São Paulo", "uf": "SP", "usuarios": 4, "denuncias": 3, "abertas": 2, "emAndamento": 0,
  "resolvidas": 1, "percentualResolvidas": 33.3, "votos": 3, "comentarios": 0,
  "confirmacoesDeResolucao": 0, "temasDeDireitos": 5, "denunciasSimuladas": 0 }
```

`denunciasSimuladas` diz quantas das denúncias da cidade são conteúdo de exemplo (seção 8.10).

`GET /relatorios/categorias`:

```json
[ { "categoria": { "codigo": "ASFALTO", "descricao": "Asfalto" },
    "total": 1, "abertas": 1, "emAndamento": 0, "resolvidas": 0, "percentualResolvidas": 0.0 } ]
```

`GET /relatorios/bairros`:

```json
[ { "bairro": "Centro", "total": 2, "resolvidas": 1, "percentualResolvidas": 50.0, "saldoVotos": 1 } ]
```

Denúncias sem bairro aparecem como `"bairro": "Não informado"`.

`GET /relatorios/engajamento`:

```json
[ { "id": 2, "titulo": "Cratera enorme na Avenida Brasil",
    "categoria": { "codigo": "ASFALTO", "descricao": "Asfalto" },
    "status": { "codigo": "ABERTA", "descricao": "Em aberto" },
    "votos": 2, "saldo": 2, "comentarios": 0, "engajamento": 2 } ]
```

Use o `id` para abrir o detalhe (`GET /denuncias/{id}`).

### 8.10 "Cidade viva" (conteúdo de demonstração)

Para o sistema nunca parecer vazio — inclusive quando o avaliador se cadastrar numa cidade qualquer:

- **Primeira pessoa de uma cidade:** a API cria **12 moradores de exemplo** e **12 denúncias** com votos, comentários e confirmações dos últimos dias. Tem de tudo: crateras muito votadas, calçadas com poucos votos (mas que sobem no "Para você" de idosos e PCD), denúncias resolvidas, uma em andamento e **uma com "2 de 3 moradores confirmaram"** — esperando a 3ª confirmação.
- **Atividade contínua:** ao abrir o feed, se passaram 10 minutos desde a última atividade da cidade, surgem 1 a 3 novidades (votos, comentários, às vezes uma denúncia nova).
- Moradores de exemplo **não aparecem** em `GET /usuarios` nem em `GET /usuarios/{id}`, **não fazem login** e **nunca** votam/comentam em denúncias de pessoas reais.
- Quem **muda de cidade** no perfil (`PUT /usuarios/me`) também recebe o conteúdo de exemplo da cidade nova, se ela ainda não tiver.

**O que o front precisa fazer:**

1. **Selo "Exemplo"** no card e no comentário quando `autor.simulado` for `true`:

   ```json
   "autor": { "id": 21, "nome": "Dona Lúcia Ramos", "simulado": true }
   ```

2. **Imagens de demonstração.** As denúncias de exemplo usam `imagemUrl` no formato `img/demo/<categoria>-<1 a 3>.jpg`. Coloque estes 18 arquivos em `public/img/demo/`:

   | Categoria | Arquivos |
   |---|---|
   | Asfalto | `asfalto-1.jpg`, `asfalto-2.jpg`, `asfalto-3.jpg` |
   | Iluminação | `iluminacao-1.jpg` … `iluminacao-3.jpg` |
   | Calçada e acessibilidade | `calcada-acessibilidade-1.jpg` … `-3.jpg` |
   | Segurança | `seguranca-1.jpg` … `-3.jpg` |
   | Limpeza urbana | `limpeza-urbana-1.jpg` … `-3.jpg` |
   | Outros | `outros-1.jpg` … `-3.jpg` |

   Use as mesmas imagens na tela "Adicionar foto" da nova denúncia (o usuário escolhe uma; envie o caminho em `imagemUrl`).

3. **"Preparando sua cidade…"** após o cadastro (seção 8.2).

`GET /relatorios/resumo` também traz `denunciasSimuladas` (quantas das denúncias da cidade são de exemplo), para transparência.

> Para a equipe: existem rotas de preparo/reset da demonstração em `/api/simulacao/*`, protegidas por uma chave configurada no `db.properties` (`simulacao.chave`). **O front não usa essas rotas.**

---

## 9. Objetos devolvidos pela API

### `DenunciaResponse`

```json
{
  "id": 2, "protocolo": "2026-0002",
  "titulo": "Cratera enorme na Avenida Brasil",
  "descricao": "Já danificou vários carros nesta semana.",
  "categoria": { "codigo": "ASFALTO", "descricao": "Asfalto" },
  "status": { "codigo": "ABERTA", "descricao": "Em aberto" },
  "endereco": "Avenida Brasil, 900", "bairro": "Jardim América",
  "cidade": "São Paulo", "uf": "SP",
  "imagemUrl": "img/demo/asfalto-1.jpg",
  "autor": { "id": 3, "nome": "Ana Martins", "simulado": false },
  "placar": { "upvotes": 2, "downvotes": 0, "saldo": 2, "meuVoto": "UP" },
  "comentarios": 0,
  "resolucao": { "confirmacoes": 0, "necessarias": 3, "euConfirmei": false },
  "relevanteParaMim": false,
  "criadaEm": "2026-11-11T01:29:06", "atualizadaEm": "2026-11-11T17:05:22"
}
```

| Campo | Observação |
|---|---|
| `bairro`, `imagemUrl` | Só aparecem se foram informados |
| `placar.meuVoto` | Só aparece se quem consulta votou (`"UP"` / `"DOWN"`) |
| `relevanteParaMim` | Só aparece no **feed** |
| Selo do card ("ASFALTO · EM ABERTO") | `categoria.descricao` + `status.descricao` |

### `PaginaResponse`

```json
{ "pagina": 1, "tamanho": 10, "total": 37, "temProxima": true, "itens": [ ... ] }
```

### Outros

| Objeto | Campos |
|---|---|
| `UsuarioResponse` | `id, nome, email, cidade, uf, criadoEm, perfilPreenchido, cidadePreparada?` (no cadastro e quando a cidade muda em `PUT /usuarios/me`) |
| `AutorResponse` (em denúncias e comentários) | `id, nome, simulado` |
| `PlacarResponse` | `upvotes, downvotes, saldo, meuVoto?` |
| `ComentarioResponse` | `id, idDenuncia, texto, autor {id, nome, simulado}, meu, criadoEm, editadoEm?` |
| `DireitoResumoResponse` | `id, titulo, resumo, icone` |
| `PerfilCompletoResponse` | `perfil {...}, tags [{codigo, descricao}], direitos [DireitoResumoResponse]` |
| `OpcaoResponse` (enums) | `codigo, descricao` |

---

## 10. Dicas de interface

- **"há 2 h"** (cards e comentários): calcule a partir de `criadaEm`/`criadoEm` com `Intl.RelativeTimeFormat('pt-BR')`.
- **Iniciais do avatar** ("MO"): primeira letra do primeiro e do último nome de `autor.nome`.
- **"MORADORA · SÃO PAULO"** (tela 19): monte com `cidade` do usuário.
- **Imagens das denúncias:** não há upload. Guarde as imagens de demonstração no front (ex.: `public/img/demo/asfalto-1.jpg`) e envie o caminho em `imagemUrl`. Sem `imagemUrl`, mostre uma imagem padrão pela categoria.
- **Canais oficiais (tela 32):** são fixos no front; use também `linkOficial` do tema, quando houver.
- **Linguagem cautelosa:** os textos de direitos dizem "possibilidade a verificar" — mantenha esse tom nos títulos das telas.

---

## 11. Pedidos do back-end para o front-end

Ajustes em relação ao Figma, combinados com o grupo. **Todos foram atendidos no front-end** (ver `frontend/README.md`):

1. **Aba "Para você" no feed** (além de Recentes e Mais votados). É ela que mostra o cruzamento perfil × denúncia — o diferencial do projeto no vídeo.
2. **Nova denúncia: separar "Endereço" e "Bairro"** em dois campos.
3. **Cidade como `<select>`** (cadastro e editar perfil), enviando `cidade` e `uf` separados. Com texto livre, "Sao Paulo" e "São Paulo" viram cidades diferentes e o feed fica vazio.
4. **Questionário: pergunta opcional "Sua família está no CadÚnico / recebe benefício social?"** → `baixaRenda: true/false`. Sem ela, BPC, ID Jovem e Tarifa Social nunca aparecem.
5. **Foto:** escolher entre imagens de demonstração e enviar só o caminho (`imagemUrl`).
6. **Resolver denúncias:** em "Minha denúncia", botão **"Marcar como resolvida"** (vira **"Reabrir"**); no detalhe, para os outros moradores, botão **"Já foi resolvido"** com o texto **"2 de 3 moradores confirmaram"**.
7. *(Opcional)* Selo **"Para você"** no card quando `relevanteParaMim` for `true`.
8. **Bloco "Números da cidade"** (ex.: no topo do feed) com `GET /relatorios/resumo` — mostra impacto (% resolvidas) e conta como diferencial "Relatórios com SQL" na avaliação.
9. *(Opcional)* No Perfil, opções **"Trocar senha"** e **"Excluir minha conta"** (com confirmação de senha).
10. **Selo "Exemplo"**, **18 imagens de demonstração** e tela **"Preparando sua cidade…"** (seção 8.10).

---

## 12. Regras da FIAP que afetam o front

- **Tecnologias fora do conteúdo não pontuam:** usar **Bootstrap** (Tailwind não está no conteúdo) e **`fetch`** (axios não está no conteúdo).
- **Checklist da entrega:** "**O index.html abre direto no navegador**". Com Vite, isso exige `base: './'` no `vite.config.js` e **`HashRouter`** no React Router, entregando a pasta `dist/`.
- **Rubrica Web:** pelo menos 3 páginas com o mesmo menu (a barra inferior Cidade · Meus Direitos · Perfil atende), validação em JavaScript e mensagens na página (use `campos` dos erros da API).

---

## 13. Histórico de mudanças

| Data | Mudança |
|---|---|
| 11/11/2026 | Primeira versão: usuários, login, perfil, direitos, denúncias, feed, votos, comentários e resolução pela comunidade. |
| 11/11/2026 | Relatórios da cidade (§8.9), trocar senha e excluir conta (§8.2). Pedidos 8 e 9 ao front. |
| 11/11/2026 | "Cidade viva" (§8.10): `autor.simulado`, `cidadePreparada`, `denunciasSimuladas`, imagens de demonstração. Pedido 10 ao front. |
| 11/11/2026 | Cadastro na primeira conta de uma cidade caiu de ~7,5 s para ~1–2 s (conteúdo de exemplo gravado em lote). Sem mudança de contrato. |
| 11/11/2026 | Robustez: `imagemUrl` validada, `busca` até 100 caracteres, escrita no catálogo de direitos exige `X-Chave-Admin`. |
| 11/11/2026 | Cidade comparada sem acento, maiúsculas e espaços repetidos em feed, busca, relatórios e confirmação ("Sao Paulo" = "São Paulo"). Coleção do Insomnia em `docs/insomnia/`. |
| 12/11/2026 | `database/script.sql` com as tabelas e o catálogo inicial de 5 temas de direitos, iguais ao schema do Oracle da FIAP; o script só cria o que falta e não apaga nada. Erros 405 e 415 na tabela da seção 6. Sem mudança de contrato. |
| 12/11/2026 | Mudar de cidade no perfil prepara a "cidade viva" da cidade nova; `GET /usuarios/{id}` responde 404 para morador de exemplo; o pulso de atividade não roda em dobro com acessos simultâneos. |
| 12/11/2026 | Front-end concluído e testado de ponta a ponta contra a API e o Oracle (inclusive com o `index.html` aberto por `file://`). Todos os pedidos da seção 11 atendidos. Sem mudança de contrato. |
