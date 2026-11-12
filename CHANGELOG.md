# Histórico de mudanças

As principais entregas do projeto, da mais recente para a mais antiga. O detalhe de cada mudança está no histórico do git (`git log`), e as mudanças de contrato da API estão na [§13 do `docs/api.md`](docs/api.md#13-histórico-de-mudanças).

## 12/11/2026 · Correções da revisão da documentação

- **"Apagar minhas respostas"** em Meus Direitos: apaga só o questionário (e o rascunho dele no navegador), como a caixa de consentimento promete.
- Comentário oculto por 3 reportes não pode mais ser editado nem reportado de novo (404); o autor ainda pode excluí-lo.
- Relatórios com só `cidade` ou só `uf` respondem 400 no campo que faltou, em vez de ignorar o valor.
- Token de uma conta excluída passa a valer como visitante também nas rotas públicas.

## 12/11/2026 · Segurança, moderação, desempenho e funcionalidades novas

**Segurança e privacidade**
- Sessão por **token assinado** (HMAC-SHA256, 7 dias) no cabeçalho `Authorization: Bearer`, no lugar do antigo `X-Usuario-Id`.
- Login bloqueado por 15 minutos depois de 5 senhas erradas seguidas (HTTP 429).
- `imagemUrl` aceita só as fotos de demonstração do app (`img/demo/...`).
- Cabeçalhos de segurança em todas as respostas (`nosniff`, `no-referrer`, `DENY`, `Cache-Control`).
- Consentimento obrigatório para salvar dados sensíveis do questionário (LGPD).

**Moderação**
- Reporte de comentário ofensivo: com 3 reportes de pessoas diferentes, o comentário some da conversa.
- Comentários com palavrões e ofensas comuns são recusados.

**Desempenho**
- Pool de conexões com o Oracle feito só com o JDK.
- Índices baseados em função para a busca por cidade.
- A API escuta também em IPv6, e o `localhost` deixou de atrasar cerca de 2 s no Windows.
- As listas fixas (categorias, situações etc.) ficam em cache no navegador por um dia.

**Funcionalidades**
- Tela **Painel da cidade**: resumo, denúncias por categoria, bairros com mais problemas e denúncias com mais participação.
- Busca por texto e filtros de categoria e situação no feed.
- "Usar minha localização" para preencher o endereço da nova denúncia.
- Avisos de novidades em "Minhas denúncias" (apoios, comentários e mudanças de situação desde a última visita).
- Compartilhar denúncia pelo menu do celular ou copiando o link.

**Qualidade**
- CI no GitHub Actions, teste de ponta a ponta da API e testes de integração dos DAOs com ROLLBACK.
- Documentação revisada: guia para agentes de IA, guia de contribuição, arquitetura, segurança e privacidade, banco de dados e roteiro de demonstração.

## 12/11/2026 · Front-end completo e banco

- App React com todas as telas do Figma: boas-vindas, cadastro em 4 etapas, login, feed com abas, detalhe com conversa do bairro, nova denúncia em etapas, minhas denúncias, questionário, temas de direitos, canais oficiais e perfil.
- Acessibilidade e movimento revisados em todas as telas (foco a cada troca de tela, etapas na URL, rascunhos, voto otimista, `prefers-reduced-motion`).
- Canais oficiais com a prefeitura da cidade (OpenStreetMap e Wikidata), o Gov.br e os CRAS próximos.
- Fotos brasileiras de demonstração (Wikimedia Commons), com créditos.
- `frontend/dist/` versionada, abrindo direto do disco (`file://`).
- `database/script.sql` que só cria o que falta e convive com outro projeto no mesmo schema; diagrama ER.

## 11/11/2026 · API completa

- Usuários, login com senha PBKDF2, questionário com tags demográficas e catálogo de direitos.
- Denúncias, feed ranqueado por perfil e votos, votos, conversa do bairro e resolução confirmada por três moradores.
- Relatórios da cidade, troca de senha e exclusão da conta.
- "Cidade viva": conteúdo de demonstração gerado para a primeira pessoa de cada cidade, com pulsos de atividade.
- Guia da API para o front-end, coleção do Insomnia, README do back-end e diagrama de classes.

## 10/11/2026 · Início

- Monorepo com `backend/`, `frontend/`, `database/` e `docs/`.
- Projeto Maven com Jersey e Grizzly, configuração por `db.properties` ou variáveis de ambiente, CORS, hierarquia de exceções com respostas de erro padronizadas, validador de campos, acesso ao Oracle com transações e endpoint de saúde.
