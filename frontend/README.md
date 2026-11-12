# Meus Direitos · Front-end

Interface web do projeto **Meus Direitos** (FIAP Global Solution 2026/2), feita a partir do protótipo do Figma (33 telas mobile).
Ela consome a API REST do [back-end](../backend/README.md); o contrato está em [`docs/api.md`](../docs/api.md).

## 1. Pré-requisitos

| Ferramenta | Versão |
|---|---|
| Node.js | 20 ou superior (testado com o 24) |
| npm | 10 ou superior |
| API do back-end | rodando em `http://localhost:8080/api` (ver [`backend/README.md`](../backend/README.md)) |

## 2. Como executar

Na pasta `frontend/`:

```bash
npm install
npm run dev
```

Abra `http://localhost:5173`. A API já libera essa origem no CORS.

Para usar outra URL da API, crie um arquivo `.env.local` na pasta `frontend/`:

```properties
VITE_API_URL=http://localhost:8080/api
```

## 3. Entrega: abrir o `index.html` direto no navegador

O checklist da FIAP pede que o `index.html` abra sem servidor. Para isso:

```bash
npm run build
```

e abra `dist/index.html` no navegador (duplo clique).

O projeto já está preparado para isso:
- `base: './'` no `vite.config.js`: todos os caminhos são relativos;
- `HashRouter`: as rotas ficam depois do `#` (`index.html#/cidade`), sem precisar de servidor;
- o build gera **um único script comum** (sem `type="module"`), porque o navegador recusa módulos abertos por `file://`.

**CORS ao abrir por `file://`.** Nesse caso o navegador envia a origem `null`. Inclua `null` na lista de origens da API, no `backend/db.properties`:

```properties
cors.origens=http://localhost:5173,null
```

Isso só é necessário para a avaliação com o `index.html` aberto do disco. A identificação vai num cabeçalho (`X-Usuario-Id`), não num cookie, então liberar `null` não permite que outra página use a sessão de alguém.

## 4. Tecnologias

Apenas o que está no conteúdo do curso:

| Tecnologia | Uso |
|---|---|
| React + Vite | Componentes e build |
| React Router (`HashRouter`) | Navegação entre as telas |
| Bootstrap 5 | Reset, grade e utilitários (`d-flex`, `gap-*`, `form-control`…), com o tema do Figma por cima |
| `fetch` | Chamadas à API (sem axios) |

## 5. Organização

```
src/
  main.jsx              entrada: Bootstrap, tema, HashRouter e sessão
  App.jsx               mapa de rotas (com o número de cada tela do Figma)
  styles/tema.css       cores, tipografia e componentes do Figma (classes md-*)
  services/api.js       cliente da API: X-Usuario-Id, erros padronizados (ErroApi)
  services/sessao.js    login guardado no localStorage
  contexts/             useSessao(): usuário logado em qualquer tela
  utils/                formatação ("há 2 h", iniciais) e validação dos formulários
  components/           peças reutilizadas: Tela, Cabecalho, Campo, CardDenuncia, NavegacaoPrincipal…
  pages/                uma pasta por seção do Figma
    inicio/             01 Boas-vindas, 02–05 Cadastro, 06 Entrar
    cidade/             07–11 Feed, votos e detalhe com comentários
    denuncias/          12–18 Nova denúncia, minhas denúncias, excluir
    perfil/             19–21 Perfil, editar, trocar senha, excluir conta
    direitos/           22–33 Questionário e temas de direitos
  assets/               ícones e imagens exportados do Figma
public/img/demo/        fotos de demonstração das denúncias (caminho enviado em imagemUrl)
```

## 6. Decisões

- **Fiel ao Figma, com os ajustes combinados no guia da API** (seção 11 do `docs/api.md`): aba "Para você" no feed, cidade e UF separados, endereço e bairro separados, pergunta do CadÚnico no questionário, botões de resolução pela comunidade, "Números da cidade", trocar senha e excluir conta.
- **Validação em JavaScript antes de chamar a API**, com as mesmas regras do back-end. Os erros que ainda vierem da API aparecem ao lado de cada campo.
- **Sessão**: o login guarda o id do usuário no `localStorage` e o envia no cabeçalho `X-Usuario-Id`. Uma resposta 401 encerra a sessão e volta para "Entrar".
- **Coluna de celular**: o protótipo é mobile (390 px). No computador, a interface aparece centralizada com até 430 px.
- **Acessibilidade**: rótulos em todos os campos, erros anunciados (`role="alert"`), botões só com ícone com `aria-label` e foco visível.
