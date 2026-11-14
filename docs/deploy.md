# Deploy: API no Render e app na Vercel

O app fica online assim: o **front-end** (React) na **Vercel**, a **API** (Java) num container no **Render** e o **banco** continua no **Oracle da FIAP**. Os dois serviços têm plano gratuito com HTTPS e publicam de novo a cada push na `main`.

```
navegador ──HTTPS──▶ Vercel (frontend/)  ──HTTPS, fetch──▶ Render (backend/, Docker) ──JDBC──▶ Oracle da FIAP
```

A ordem importa: o Render gera o endereço da API, que a Vercel precisa no build; a Vercel gera o endereço do app, que a API precisa liberar no CORS.

## 1. API no Render

1. Em [render.com](https://render.com), entre com o GitHub e escolha **New → Blueprint**. Selecione este repositório: o Render lê o [`render.yaml`](../render.yaml) da raiz e cria o serviço `meus-direitos-api` (Docker, plano gratuito, health check em `/api/health`).
2. Ele pede os valores marcados como secretos. Preencha:

   | Variável | Valor |
   |---|---|
   | `MD_DB_USUARIO` | usuário do Oracle da FIAP |
   | `MD_DB_SENHA` | senha do Oracle da FIAP |
   | `MD_CORS_ORIGENS` | por enquanto `http://localhost:5173` (troca no passo 3) |

   `MD_TOKEN_SEGREDO`, `MD_SIMULACAO_CHAVE` e `MD_ADMIN_CHAVE` são gerados pelo próprio Render. Para ver as chaves (Insomnia, preparo da demonstração), abra o serviço → **Environment**.
3. O primeiro build leva alguns minutos (o Maven baixa as dependências). No fim, abra `https://<seu-servico>.onrender.com/api/health`. Deve aparecer `"banco":"UP"`.
   - `"banco":"INDISPONIVEL"`: usuário ou senha errados, ou o Oracle da FIAP recusou a conexão vinda do Render. Confira as variáveis e veja os **Logs** do serviço.

Sem Blueprint também funciona: **New → Web Service**, este repositório, **Language: Docker**, **Dockerfile Path** `./backend/Dockerfile`, **Docker Build Context** `./backend`, **Health Check Path** `/api/health` e as mesmas variáveis.

## 2. App na Vercel

1. Em [vercel.com](https://vercel.com), entre com o GitHub e escolha **Add New → Project**, importando este repositório.
2. Em **Root Directory**, escolha `frontend`. O resto vem do [`frontend/vercel.json`](../frontend/vercel.json) (Vite, `npm ci`, `npm run build`, pasta `dist`).
3. Em **Environment Variables**, crie:

   | Variável | Valor |
   |---|---|
   | `VITE_API_URL` | `https://<seu-servico>.onrender.com/api` (o endereço do passo 1, terminando em `/api`) |

4. **Deploy.** A Vercel mostra o endereço do app, por exemplo `https://meus-direitos.vercel.app`.

O `VITE_API_URL` é lido no **build**. Se o endereço da API mudar, troque a variável e faça um **Redeploy**.

## 3. Ligar os dois (CORS)

No Render, em **Environment**, troque `MD_CORS_ORIGENS` pelo endereço do app, sem barra no fim. Para continuar desenvolvendo e avaliando pelo disco, mantenha as outras origens:

```
https://meus-direitos.vercel.app,http://localhost:5173,null
```

Salvar reinicia a API. Pronto: abra o app pela Vercel e crie uma conta.

## Conferência rápida

- [ ] `https://<api>/api/health` responde `"banco":"UP"`.
- [ ] O app abre na Vercel, o cadastro funciona e a cidade já vem com denúncias de exemplo.
- [ ] Recarregar a página numa tela interna (ex.: `/#/direitos`) continua funcionando (o app usa `#` nas rotas, então não há 404).
- [ ] No console do navegador não aparece erro de CORS.
- [ ] Teste de ponta a ponta contra a API publicada: `node testes/api-ponta-a-ponta.mjs https://<api>/api` (cria contas de teste e as exclui no fim).

## O que saber no dia a dia

- **A API gratuita do Render "dorme"** depois de ~15 minutos sem uso. A primeira requisição depois disso leva perto de 1 minuto. **Antes de gravar o vídeo ou de uma apresentação, abra `/api/health` e espere responder.**
- **Cada push na `main` publica de novo** os dois serviços. A CI do GitHub continua rodando os testes e conferindo a `frontend/dist`.
- **A `frontend/dist` do repositório continua apontando para `http://localhost:8080/api`**: ela é a versão da entrega, aberta pelo disco junto com a API local. A Vercel faz o próprio build com o `VITE_API_URL` dela.
- **Segredos só no painel** do Render e da Vercel: nunca no git, no `render.yaml` ou no `vercel.json`.
- **A imagem da API** ([`backend/Dockerfile`](../backend/Dockerfile)) compila com o Maven e roda só com o Java 17 (sem dependência nova). O `.dockerignore` impede que o `db.properties` entre na imagem.
- **Sem o Render**, qualquer serviço que rode containers serve: basta a variável `PORT` (a API escuta em `0.0.0.0` nessa porta) e as variáveis `MD_*`.
