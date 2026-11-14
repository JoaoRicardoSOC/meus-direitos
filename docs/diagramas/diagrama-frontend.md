# Diagrama do front-end

Gerado a partir do código em `frontend/src` (rotas em `App.jsx`). O GitHub desenha os diagramas abaixo direto nesta página.

O diagrama está dividido em duas partes:
1. **Telas e navegação:** as rotas, os números das telas do Figma e por onde a pessoa passa.
2. **Camadas:** como uma tela chega à API.

A tabela completa de rotas (com o arquivo de cada tela) e as capturas estão no [README do front-end](../../frontend/README.md).

## 1. Telas e navegação

```mermaid
flowchart LR
    subgraph publicas["Sem login (RotaPublica)"]
        BV["/ · 01 Boas-vindas"]
        CAD["/cadastro?passo=1..4 · 02–05 Cadastro"]
        ENT["/entrar · 06 Entrar"]
    end

    subgraph cidade["Aba Cidade"]
        FEED["/cidade · 07 / 33 Feed (abas ou busca e filtros)"]
        PAINEL["/cidade/painel · Painel da cidade"]
        DET["/denuncias/:id · 10–11 Detalhe e conversa"]
        NOVA["/denuncias/nova?passo=categoria, foto ou publicada · 12–16 Nova denúncia"]
        EDIT["/denuncias/:id/editar · formulário da 12/13"]
    end

    subgraph direitos["Aba Meus Direitos"]
        MD["/direitos · 22 Primeiro acesso ou 28 Temas"]
        QUEST["/direitos/questionario?passo=1..3 · 23–27"]
        TEMA["/direitos/:id · 29–31 Detalhe do tema"]
        CANAIS["/direitos/canais · 32 Canais oficiais"]
        TODOS["/direitos/temas · Todos os temas"]
    end

    subgraph perfil["Aba Perfil"]
        PERF["/perfil · 19 Perfil"]
        EDP["/perfil/editar · 20–21"]
        SENHA["/perfil/senha"]
        EXC["/perfil/excluir"]
        MINHAS["/minhas · 17–18 Minhas denúncias"]
    end

    OUTRA["qualquer outra rota"] --> BV
    BV --> CAD --> FEED
    BV --> ENT -- "ou a tela pedida antes do login" --> FEED
    FEED --> DET
    FEED -- "Ver painel completo" --> PAINEL -- "denúncias com mais participação" --> DET
    FEED -- "botão Nova denúncia" --> NOVA -- "Ver minha denúncia" --> DET
    FEED -- "avatar" --> PERF
    MINHAS --> EDIT --> MINHAS
    MINHAS --> DET
    MINHAS -- "lista vazia" --> NOVA
    MD --> QUEST --> MD
    MD --> TEMA --> CANAIS
    MD -- "sem temas" --> CANAIS
    MD -- "Ver todos os temas" --> TODOS --> TEMA
    PERF --> EDP
    PERF --> SENHA
    PERF --> EXC --> BV
    PERF --> MINHAS
    PERF -- "Sair da conta" --> BV
```

> **Fluxos recentes:** no feed, a busca e os filtros de categoria e situação trocam as abas por `GET /denuncias` com filtros (e voltam iguais do detalhe); o "Ver painel completo" leva ao Painel da cidade (relatórios da API). Na nova denúncia, "Usar minha localização" preenche endereço e bairro. Ao abrir "Minhas denúncias", cada card mostra o que mudou desde a última visita (o Perfil mostra quantas têm novidades). No detalhe, "Compartilhar" usa o menu do sistema, copia o link ou o mostra num campo, e cada comentário de outra pessoa pode ser denunciado.

> Todas as telas logadas ficam dentro de `RotaProtegida`; as telas de aba mostram a navegação principal (Cidade · Meus Direitos · Perfil). A aba Cidade fica ativa em `/cidade/*` e `/denuncias/*`, e a aba Perfil em `/perfil/*` e `/minhas`. Sem sessão, a rota protegida leva para "Entrar" (e volta à tela pedida depois do login); depois de sair ou excluir a conta, leva para as boas-vindas. Quem já está logado e abre uma rota pública vai para `/cidade`. As etapas do cadastro, da nova denúncia e do questionário ficam na URL (`?passo=`; no questionário, também `&lista=` para as listas 24, 26 e 27), por isso o "voltar" do celular volta uma etapa.

## 2. Camadas

```mermaid
flowchart TB
    main["main.jsx<br/>Bootstrap (reset + utilitários) · tema.css · HashRouter · SessaoProvider"] --> App["App.jsx<br/>rotas · RotaProtegida/RotaPublica · GerenciarRota (topo e foco a cada tela) · PainelApresentacao"]
    App --> Telas["pages/&lt;seção&gt;/*.jsx<br/>uma tela por rota, com subcomponentes e CSS da seção"]
    Telas --> Comp["components/<br/>Tela · Cabecalho · Campo · CampoSenha · CampoCidade · CardDenuncia · NavegacaoPrincipal · Mensagem/Carregando/Toast …"]
    Telas --> Utils["utils/<br/>validacao · formatos · foco"]
    Telas --> Novidades["utils/novidades.js<br/>retrato das minhas denúncias no localStorage"]
    Telas --> Sessao["contexts/SessaoContext<br/>useSessao(): usuario, logado, entrar, sair"]
    Telas --> Api["services/api.js<br/>fetch + Authorization: Bearer + ErroApi"]
    Comp --> Api
    Comp --> Sessao
    Sessao --> Arm["services/sessao.js<br/>localStorage (com cópia em memória)"]
    Api --> Arm
    Api -- "401 encerra a sessão" --> Arm
    Api -- "HTTP JSON" --> Backend[("API REST<br/>localhost:8080/api")]
    Telas --> Loc["services/localizacao.js<br/>geolocalização + Nominatim/Wikidata"]
    Loc -. "tela 32 e nova denúncia" .-> Externos["OpenStreetMap · Wikidata · Google Maps"]
```

| Pasta | Responsabilidade |
|---|---|
| `pages/` | Uma pasta por seção do Figma (`inicio`, `cidade`, `denuncias`, `perfil`, `direitos`), com as telas, os subcomponentes e o CSS de cada uma |
| `components/` | Peças reutilizadas em várias telas, sem regras de uma tela específica (inclui as guardas de rota, o `GerenciarRota` e o `PainelApresentacao`) |
| `services/` | Comunicação com a API (`api.js`), sessão do usuário (`sessao.js`) e localização (`localizacao.js`: navegador e OpenStreetMap) |
| `contexts/` | Estado global de sessão (`useSessao()`) |
| `utils/` | Funções puras: validação (as mesmas regras do back-end), formatação, foco e novidades das minhas denúncias (`novidades.js`, com o retrato no `localStorage`) |
| `styles/tema.css` | Tokens e classes do Figma, larguras (celular, folha, painel), movimento e acessibilidade |
