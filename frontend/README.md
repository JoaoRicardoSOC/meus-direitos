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

O checklist da FIAP pede que o `index.html` abra sem servidor. A pasta **`dist/` já vem pronta no repositório**: com a API rodando, abra `dist/index.html` no navegador (duplo clique).

Depois de mudar o código, gere a `dist/` de novo antes de entregar:

```bash
npm run build
```

Esse caminho foi testado de ponta a ponta com a API e o Oracle reais, abrindo o `index.html` pelo `file://`: cadastro, feed, votos, comentários, resolução, nova denúncia, questionário, canais oficiais e exclusão da conta.

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
| Bootstrap 5 | Só o reset e os utilitários de layout (`d-flex`, `gap-*`, `m-*`…); campos, botões e o resto vêm do tema do Figma |
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
- **Coluna de celular**: o protótipo é mobile (390 px). A partir de 768 px o app aparece como uma folha de 430 px com moldura; a partir de 1100 px, com um painel de apresentação do projeto ao lado.
- **Fonte Inter no próprio projeto** (`src/assets/fontes`, licença OFL): funciona offline e no `file://`, sem enviar o IP de quem usa a terceiros.
- **Acessibilidade**: foco visível em tudo, a cada tela nova a página volta ao topo e o foco vai para o título, erros de formulário com um resumo único e foco no primeiro campo, alvos de toque de 44 px, contraste AA (inclusive bordas dos campos) e suporte a "reduzir movimento".
- **Etapas na URL**: cadastro, nova denúncia e questionário guardam a etapa no endereço (`?passo=`), então o "voltar" do celular volta uma etapa; o que foi digitado fica salvo na sessão do navegador.
- **Movimento**: retorno ao toque nos controles, voto que muda na hora (e volta atrás se a rede falhar), entrada das etapas e confirmações animadas, tudo abaixo de 300 ms.
- **Canais oficiais (tela 32) pela localização**: antes de buscar, a pessoa escolhe entre a **localização atual** (o navegador pede permissão) e a **cidade do cadastro**. A escolha vale para a sessão e pode ser trocada na própria tela.
  - *Portal da Prefeitura*: as coordenadas viram cidade e UF no [Nominatim/OpenStreetMap](https://nominatim.org/), que muitas vezes já traz o site oficial do município. Sem ele, o site vem do [Wikidata](https://www.wikidata.org/) (propriedade "site oficial"); sem nenhum dos dois, abre uma busca por "Prefeitura de {cidade}".
  - *Portal Gov.br*: sempre `https://www.gov.br`.
  - *Atendimento presencial*: abre o Google Maps buscando "CRAS" em volta da posição.
  - Com a cidade do cadastro, ou se a localização for negada ou não vier, tudo usa a cidade do cadastro, e a tela explica o motivo.
  - A posição é enviada só ao OpenStreetMap (e, no link do CRAS, ao Google Maps, quando a pessoa clica); nada vai para a API do Meus Direitos. O resultado fica guardado na sessão do navegador para não repetir pedidos (a política do Nominatim é de no máximo 1 por segundo).

## 7. Créditos das imagens

As fotos de demonstração das denúncias ficam em `public/img/demo/` (800 px de largura) e as miniaturas da escolha de foto em `public/img/demo/mini/` (400 px). Elas mostram problemas urbanos parecidos com os de cada categoria, mas **não são fotos dos casos registrados no app**: servem só para ilustrar as denúncias de exemplo e a escolha de foto ao criar uma denúncia.

Todas vêm do Wikimedia Commons ou do Flickr, com licença que permite uso, inclusive comercial, desde que o autor seja creditado. 14 das 18 são do Brasil. As fotos foram apenas redimensionadas, sem outras alterações.

| Arquivo | Título original | Autor | Licença | Fonte |
|---|---|---|---|---|
| `asfalto-1.jpg` | Ceará (6276101325) | Agencia CNT de Noticias | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Cear%C3%A1_(6276101325).jpg) |
| `asfalto-2.jpg` | Buraco na rua Licínio Felini- São Paulo | Rodrigo Tetsuo Argenton | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Buraco_na_rua_Lic%C3%ADnio_Felini-_S%C3%A3o_Paulo.JPG) |
| `asfalto-3.jpg` | Acre - BR-317 (pavimento) (8102615312) | Agencia CNT de Noticias | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Acre_-_BR-317_(pavimento)_(8102615312).jpg) |
| `iluminacao-1.jpg` | A lamp post crushed at Avenida Las Delicias, Maracay, Venezuela | Bobjgalindo | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:A_lamp_post_crushed_at_Avenida_Las_Delicias,_Maracay,_Venezuela.jpg) |
| `iluminacao-2.jpg` | Rua José Batista à noite, Rio Vermelho, Trindade, novembro de 2019 | Fronteira | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Rua_Jos%C3%A9_Batista_%C3%A0_noite,_Rio_Vermelho,_Trindade,_novembro_de_2019.jpg) |
| `iluminacao-3.jpg` | A fallen street light pole | Niera | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:A_fallen_street_light_pole.jpg) |
| `calcada-acessibilidade-1.jpg` | Calçada quebrada | Mílton Jung | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [página (Flickr)](https://www.flickr.com/photos/22155587@N02/5533517024) |
| `calcada-acessibilidade-2.jpg` | Calçada quebrada na Praça Cidade de Milão | Mílton Jung | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [página (Flickr)](https://www.flickr.com/photos/22155587@N02/3802112993) |
| `calcada-acessibilidade-3.jpg` | Damaged sidewalk in Montevideo - 02 | NaBUru38 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Damaged_sidewalk_in_Montevideo_-_02.jpg) |
| `seguranca-1.jpg` | Prédio abandonado no centro histórico de Cananéia | Ferik80 | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Pr%C3%A9dio_abandonado_no_centro_hist%C3%B3rico_de_Canan%C3%A9ia.jpg) |
| `seguranca-2.jpg` | RUA DESERTA | Guilhermedesc | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:RUA_DESERTA.jpg) |
| `seguranca-3.jpg` | Rua Dom Joaquim Silvério à noite, Coração Eucarístico, Belo Horizonte, fevereiro de 2023 (1) | Fronteira | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Rua_Dom_Joaquim_Silv%C3%A9rio_%C3%A0_noite,_Cora%C3%A7%C3%A3o_Eucar%C3%ADstico,_Belo_Horizonte,_fevereiro_de_2023_(1).jpg) |
| `limpeza-urbana-1.jpg` | Lixo na calçada da Taquari | Mílton Jung | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [página (Flickr)](https://www.flickr.com/photos/22155587@N02/6333060344) |
| `limpeza-urbana-2.jpg` | Lixo na calçada | Mílton Jung | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [página (Flickr)](https://www.flickr.com/photos/22155587@N02/8557470163) |
| `limpeza-urbana-3.jpg` | Lixo Nas Ruas - Marabá - Pa - panoramio | FernandoSantos | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Lixo_Nas_Ruas_-_Marab%C3%A1_-_Pa_-_panoramio.jpg) |
| `outros-1.jpg` | Enorme raiz de uma árvore caida na praça - panoramio | Paulo Humberto | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Enorme_raiz_de_uma_%C3%A1rvore_caida_na_pra%C3%A7a_-_panoramio.jpg) |
| `outros-2.jpg` | Rua Marcílio Dias, em Porto Alegre, durante a enchente de maio de 2024 | Apesito.nomas | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:Rua_Marc%C3%ADlio_Dias,_em_Porto_Alegre,_durante_a_enchente_de_maio_de_2024.jpg) |
| `outros-3.jpg` | A photo of a water leak on Camberwell Place 2022-10-08 9 | Thomas Booker (CoderThomasB) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página (Wikimedia Commons)](https://commons.wikimedia.org/wiki/File:A_photo_of_a_water_leak_on_Camberwell_Place_2022-10-08_9.jpg) |

## 8. Uso de IA generativa (front-end)

Usamos o **Claude (Anthropic)**, via **Claude Code**, como apoio no front-end para:
- ler as telas do protótipo no Figma (pelo servidor MCP e pela API REST do Figma) e converter o design em componentes React;
- gerar e revisar o código das telas, dos componentes e da integração com a API;
- auditar a interface com três guias de qualidade (Impeccable, tasteskill e os princípios de interação de Emil Kowalski) e aplicar as correções de acessibilidade, movimento e desempenho;
- buscar as fotos de demonstração em acervos abertos, conferindo licença e crédito de cada uma;
- escrever os testes de ponta a ponta no navegador (contra a API e o Oracle reais) e esta documentação.

O design é da equipe (protótipo da Marcela no Figma) e as mudanças em relação a ele foram aprovadas por ela. As decisões técnicas e as justificativas estão documentadas nesta página.
