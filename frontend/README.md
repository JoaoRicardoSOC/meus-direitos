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
- **Canais oficiais (tela 32) pela localização**: antes de buscar, a pessoa escolhe entre a **localização atual** (o navegador pede permissão) e a **cidade do cadastro**. A escolha vale para a sessão e pode ser trocada na própria tela.
  - *Portal da Prefeitura*: as coordenadas viram cidade e UF no [Nominatim/OpenStreetMap](https://nominatim.org/), que muitas vezes já traz o site oficial do município. Sem ele, o site vem do [Wikidata](https://www.wikidata.org/) (propriedade "site oficial"); sem nenhum dos dois, abre uma busca por "Prefeitura de {cidade}".
  - *Portal Gov.br*: sempre `https://www.gov.br`.
  - *Atendimento presencial*: abre o Google Maps buscando "CRAS" em volta da posição.
  - Com a cidade do cadastro, ou se a localização for negada ou não vier, tudo usa a cidade do cadastro, e a tela explica o motivo.
  - A posição é enviada só ao OpenStreetMap (e, no link do CRAS, ao Google Maps, quando a pessoa clica); nada vai para a API do Meus Direitos. O resultado fica guardado na sessão do navegador para não repetir pedidos (a política do Nominatim é de no máximo 1 por segundo).

## 7. Créditos das imagens

As fotos de demonstração das denúncias ficam em `public/img/demo/`. Elas mostram problemas urbanos parecidos com os de cada categoria, mas **não são fotos dos casos registrados no app**: servem só para ilustrar as denúncias de exemplo e a escolha de foto ao criar uma denúncia.

`asfalto-1.jpg` é a foto ilustrativa do protótipo do Figma. As demais vêm do Wikimedia Commons, todas com licença que permite uso, inclusive comercial, desde que o autor seja creditado. As fotos foram reduzidas para 900 px de largura, sem outras alterações.

| Arquivo | Título original | Autor | Licença | Fonte |
|---|---|---|---|---|
| `asfalto-2.jpg` | Buraco na rua Licínio Felini- São Paulo | Rodrigo Tetsuo Argenton | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [página](https://commons.wikimedia.org/wiki/File:Buraco_na_rua_Lic%C3%ADnio_Felini-_S%C3%A3o_Paulo.JPG) |
| `asfalto-3.jpg` | Hennessey Street Potholes New Orleans | Bart Everson | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [página](https://commons.wikimedia.org/w/index.php?curid=86543517) |
| `iluminacao-1.jpg` | Nossa Brasília - Postes de iluminação pública (25075634645) | Agência Brasília from Brasília, Brasil | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [página](https://commons.wikimedia.org/wiki/File:Nossa_Bras%C3%ADlia_-_Postes_de_ilumina%C3%A7%C3%A3o_p%C3%BAblica_(25075634645).jpg) |
| `iluminacao-2.jpg` | Esztergom by night 01 - Simor János utca | Pudelek | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página](https://commons.wikimedia.org/wiki/File:Esztergom_by_night_01_-_Simor_J%C3%A1nos_utca.jpg) |
| `iluminacao-3.jpg` | 2022 Cieszyn Klucki street (lamp) | Qasinka | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [página](https://commons.wikimedia.org/wiki/File:2022_Cieszyn_Klucki_street_(lamp).jpg) |
| `calcada-acessibilidade-1.jpg` | Broken sidewalk from tree root growth (Columbus, Ohio, USA) 1 | James St. John | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [página](https://commons.wikimedia.org/wiki/File:Broken_sidewalk_from_tree_root_growth_(Columbus,_Ohio,_USA)_1.jpg) |
| `calcada-acessibilidade-2.jpg` | Broken sidewalk LA | Downtowngal | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página](https://commons.wikimedia.org/wiki/File:Broken_sidewalk_LA.jpg) |
| `calcada-acessibilidade-3.jpg` | Curb cut for wheelchair ramp (DSC 3500) | Nick-philly | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página](https://commons.wikimedia.org/wiki/File:Curb_cut_for_wheelchair_ramp_(DSC_3500).jpg) |
| `seguranca-1.jpg` | Liddell Street after dark - geograph.org.uk - 8221645 | DS Pugh | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [página](https://commons.wikimedia.org/wiki/File:Liddell_Street_after_dark_-_geograph.org.uk_-_8221645.jpg) |
| `seguranca-2.jpg` | Kamran Alley - Night - Tehran | Sonia Sevilla | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [página](https://commons.wikimedia.org/wiki/File:Kamran_Alley_-_Night_-_Tehran.jpg) |
| `seguranca-3.jpg` | Alley in Scicli by night | Julian Lupyan | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [página](https://commons.wikimedia.org/wiki/File:Alley_in_Scicli_by_night.jpg) |
| `limpeza-urbana-1.jpg` | Lixo fechando a rua | Gabriel Vinicius Cabral | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | [página](https://commons.wikimedia.org/wiki/File:Lixo_fechando_a_rua.jpg) |
| `limpeza-urbana-2.jpg` | Lixo na rua | jmerelo | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0) | [página](https://commons.wikimedia.org/wiki/File:Lixo_na_rua.jpg) |
| `limpeza-urbana-3.jpg` | Lixo Nas Ruas - Marabá - Pa - panoramio | FernandoSantos | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | [página](https://commons.wikimedia.org/wiki/File:Lixo_Nas_Ruas_-_Marab%C3%A1_-_Pa_-_panoramio.jpg) |
| `outros-1.jpg` | Fallen tree, 16th Street NW | Nvss132 | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [página](https://commons.wikimedia.org/wiki/File:Fallen_tree,_16th_Street_NW.jpg) |
| `outros-2.jpg` | A photo of a water leak on Camberwell Place 2022-10-08 9 | Thomas Booker (CoderThomasB) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [página](https://commons.wikimedia.org/wiki/File:A_photo_of_a_water_leak_on_Camberwell_Place_2022-10-08_9.jpg) |
| `outros-3.jpg` | Moscow, Donskaya Street, fallen tree June 2022 01 | Retired electrician | [CC0](https://creativecommons.org/publicdomain/zero/1.0/deed.en) | [página](https://commons.wikimedia.org/wiki/File:Moscow,_Donskaya_Street,_fallen_tree_June_2022_01.jpg) |
