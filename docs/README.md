# Documentação do Meus Direitos

Comece pelo documento que corresponde ao que você quer fazer.

## Por objetivo

| Quero… | Leia |
|---|---|
| Entender o produto e rodar o projeto | [README principal](../README.md) |
| Entender como as peças se encaixam e por que foram feitas assim | [Arquitetura](arquitetura.md) |
| Integrar com a API ou testar uma rota | [Guia da API](api.md) e a [coleção do Insomnia](insomnia/meus-direitos.insomnia.json) |
| Mexer no back-end | [README do back-end](../backend/README.md) e o [diagrama de classes](diagramas/diagrama-classes.md) |
| Mexer no front-end | [README do front-end](../frontend/README.md) e o [diagrama do front](diagramas/diagrama-frontend.md) |
| Criar ou mudar tabelas | [Guia do banco de dados](banco-de-dados.md) e o [diagrama ER](diagramas/diagrama-er.md) |
| Saber quais dados pessoais existem e como são protegidos | [Segurança e privacidade](seguranca-e-privacidade.md) |
| Gravar o vídeo ou apresentar o projeto | [Roteiro de demonstração](roteiro-demonstracao.md) |
| Publicar o app online (Vercel + Render) | [Deploy](deploy.md) |
| Contribuir com código | [Como contribuir](../CONTRIBUTING.md) |
| Usar um assistente de IA no projeto | [Guia para agentes de IA](../AGENTS.md) |
| Ver o que mudou e quando | [Histórico de mudanças](../CHANGELOG.md) e a [§13 do guia da API](api.md#13-histórico-de-mudanças) |

## Todos os documentos

| Documento | Conteúdo |
|---|---|
| [`arquitetura.md`](arquitetura.md) | Contexto do sistema, camadas, caminho de uma requisição, regras do produto (feed, resolução, direitos, cidade viva, moderação) e decisões de arquitetura |
| [`api.md`](api.md) | Contrato da API REST: sessão, cliente `fetch`, erros, mapa telas → rotas, referência de cada rota, objetos devolvidos e histórico |
| [`banco-de-dados.md`](banco-de-dados.md) | Objetos do projeto no schema compartilhado, como rodar e conferir o script, como evoluir o banco, conteúdo de demonstração e erros comuns do Oracle |
| [`seguranca-e-privacidade.md`](seguranca-e-privacidade.md) | Dados pessoais e sensíveis, LGPD, armazenamento no navegador, proteções da API e limitações |
| [`roteiro-demonstracao.md`](roteiro-demonstracao.md) | Preparação, roteiro com tempos e plano B para o vídeo pitch |
| [`deploy.md`](deploy.md) | API no Render (Docker) e app na Vercel: ordem, variáveis, CORS e conferência |
| [`diagramas/diagrama-classes.md`](diagramas/diagrama-classes.md) | Diagramas de classes do back-end (domínio, camadas e erros), também em SVG |
| [`diagramas/diagrama-er.md`](diagramas/diagrama-er.md) | Modelo de dados, regras garantidas pelo banco e decisões |
| [`diagramas/diagrama-frontend.md`](diagramas/diagrama-frontend.md) | Telas, rotas e camadas do front-end |
| [`insomnia/meus-direitos.insomnia.json`](insomnia/meus-direitos.insomnia.json) | Todas as rotas, com uma pasta de erros tratados (importe em *Import → From File*) |
| [`imagens/`](imagens/) | Banner e capturas de tela usados no README |
