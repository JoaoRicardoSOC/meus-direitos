# Diagrama do front-end

Gerado a partir do código em `frontend/src`. O GitHub desenha os diagramas abaixo direto nesta página.

O diagrama está dividido em duas partes:
1. **Telas e navegação:** as rotas, os números das telas do Figma e por onde a pessoa passa.
2. **Camadas:** como uma tela chega à API.

## 1. Telas e navegação

```mermaid
flowchart LR
    subgraph publicas["Sem login (RotaPublica)"]
        BV["/ · 01 Boas-vindas"]
        CAD["/cadastro?passo=1..4 · 02–05 Cadastro"]
        ENT["/entrar · 06 Entrar"]
    end

    subgraph cidade["Aba Cidade"]
        FEED["/cidade · 07–09, 33 Feed (abas ou busca e filtros)"]
        PAINEL["/cidade/painel · Painel da cidade"]
        DET["/denuncias/:id · 10–11 Detalhe e conversa"]
        NOVA["/denuncias/nova?passo= · 12–16 Nova denúncia"]
        EDIT["/denuncias/:id/editar"]
    end

    subgraph direitos["Aba Meus Direitos"]
        MD["/direitos · 22 Primeiro acesso ou 28 Temas"]
        QUEST["/direitos/questionario?passo=1..3 · 23–27"]
        TEMA["/direitos/:id · 29–31 Detalhe do tema"]
        CANAIS["/direitos/canais · 32 Canais oficiais"]
    end

    subgraph perfil["Aba Perfil"]
        PERF["/perfil · 19 Perfil"]
        EDP["/perfil/editar · 20–21"]
        SENHA["/perfil/senha"]
        EXC["/perfil/excluir"]
        MINHAS["/minhas · 17–18 Minhas denúncias"]
    end

    BV --> CAD --> FEED
    BV --> ENT --> FEED
    FEED --> DET
    FEED -- "Ver painel completo" --> PAINEL -- "denúncias com mais participação" --> DET
    FEED -- "botão Nova denúncia" --> NOVA --> DET
    MINHAS --> EDIT
    MINHAS --> DET
    MD --> QUEST --> MD
    MD --> TEMA --> CANAIS
    PERF --> EDP
    PERF --> SENHA
    PERF --> EXC --> BV
    PERF --> MINHAS
    PERF -- "Sair da conta" --> BV
```

> **Fluxos novos:** no feed, a busca e os filtros de categoria e situação trocam as abas por `GET /denuncias` com filtros (e voltam iguais do detalhe); o "Ver painel completo" leva ao Painel da cidade (relatórios da API). Na nova denúncia, "Usar minha localização" preenche endereço e bairro. Ao abrir "Minhas denúncias", cada card mostra o que mudou desde a última visita (o Perfil mostra quantas têm novidades). No detalhe, "Compartilhar" usa o menu do sistema, copia o link ou o mostra num campo.

> Todas as telas logadas ficam dentro de `RotaProtegida` e mostram a navegação principal (Cidade · Meus Direitos · Perfil). Sem sessão, a rota protegida leva para "Entrar"; depois de sair ou excluir a conta, leva para as boas-vindas. As etapas do cadastro, da nova denúncia e do questionário ficam na URL (`?passo=`), por isso o "voltar" do celular volta uma etapa.

## 2. Camadas

```mermaid
flowchart TB
    main["main.jsx<br/>HashRouter · SessaoProvider · tema.css"] --> App["App.jsx<br/>rotas · GerenciarRota (topo e foco a cada tela)"]
    App --> Telas["pages/&lt;seção&gt;/*.jsx<br/>uma tela por rota"]
    Telas --> Comp["components/<br/>Tela · Cabecalho · Campo · CardDenuncia · NavegacaoPrincipal · Mensagem/Toast …"]
    Telas --> Utils["utils/<br/>validacao · formatos · foco"]
    Telas --> Sessao["contexts/SessaoContext<br/>useSessao(): usuario, entrar, sair"]
    Telas --> Api["services/api.js<br/>fetch + Authorization: Bearer + ErroApi"]
    Comp --> Api
    Sessao --> Arm["services/sessao.js<br/>localStorage (com cópia em memória)"]
    Api --> Arm
    Api -- "HTTP JSON" --> Backend[("API REST<br/>localhost:8080/api")]
    Telas --> Loc["services/localizacao.js<br/>geolocalização + Nominatim/Wikidata"]
    Loc -. "tela 32 e nova denúncia" .-> Externos["OpenStreetMap · Wikidata · Google Maps"]
    Utils --> Novidades["utils/novidades.js<br/>retrato das minhas denúncias no localStorage"]
```

| Pasta | Responsabilidade |
|---|---|
| `pages/` | Uma pasta por seção do Figma (`inicio`, `cidade`, `denuncias`, `perfil`, `direitos`), com as telas e os subcomponentes de cada uma |
| `components/` | Peças reutilizadas em várias telas, sem chamar regras de uma tela específica |
| `services/` | Comunicação com a API (`api.js`), sessão do usuário (`sessao.js`) e localização (`localizacao.js`: navegador e OpenStreetMap) |
| `contexts/` | Estado global de sessão |
| `utils/` | Funções puras: validação (as mesmas regras do back-end), formatação, foco e novidades das minhas denúncias (`novidades.js`, com o retrato no `localStorage`) |
| `styles/tema.css` | Tokens e classes do Figma, movimento e acessibilidade |
