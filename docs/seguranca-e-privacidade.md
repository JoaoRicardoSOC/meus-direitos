# Segurança e privacidade

O app lida com dados pessoais e, no questionário de "Meus Direitos", com **dados pessoais sensíveis** (raça/cor e deficiência, art. 5º, II da LGPD). Este documento diz quais dados existem, onde ficam, quem os vê, como a pessoa os apaga e quais proteções a API aplica. É um protótipo acadêmico: as limitações estão na última seção.

## 1. Dados tratados

| Dado | Onde fica | Obrigatório? | Para quê | Quem vê |
|---|---|---|---|---|
| Nome | `TB_GS26S2_USUARIO` | Sim | Identificar o autor de denúncias e comentários | Qualquer pessoa (é público, como numa rede social de bairro) |
| E-mail | `TB_GS26S2_USUARIO` (minúsculas, único) | Sim | Login | Só a própria pessoa |
| Senha | `TB_GS26S2_USUARIO`, **só o hash** PBKDF2-HMAC-SHA256 (120.000 iterações, salt aleatório de 16 bytes) | Sim | Login | Ninguém: a senha não é guardada nem pode ser recuperada |
| Cidade e UF | `TB_GS26S2_USUARIO` | Sim | Mostrar o feed e os relatórios da cidade | Público (aparece com o nome) |
| Data de nascimento | `TB_GS26S2_PERFIL_DEMOGRAFICO` | Não | Indicar direitos de pessoa idosa e jovem | Só a própria pessoa |
| Raça/cor | `TB_GS26S2_PERFIL_DEMOGRAFICO` | Não (**sensível**) | Indicar cotas e ações afirmativas | Só a própria pessoa |
| Deficiência | `TB_GS26S2_PERFIL_DEMOGRAFICO` | Não (**sensível**) | Indicar direitos de PCD e priorizar denúncias de acessibilidade no feed | Só a própria pessoa |
| Percurso escolar, CadÚnico | `TB_GS26S2_PERFIL_DEMOGRAFICO` | Não | Indicar cotas e benefícios | Só a própria pessoa |
| Denúncias, comentários, votos, confirmações, reportes | Tabelas de cada um | — | O próprio serviço | Denúncias e comentários são públicos; votos aparecem só como totais; reportes não aparecem |
| Localização (GPS) | **Não vai para a API** | Não | Achar a prefeitura e os CRAS próximos, preencher o endereço da denúncia | Só o navegador da pessoa e o serviço de mapas consultado (OpenStreetMap) |

O perfil demográfico **nunca** aparece para outras pessoas nem em relatórios: ele só muda a ordem do feed de quem o respondeu e os temas de direitos mostrados a essa pessoa.

## 2. Bases e direitos da pessoa (LGPD)

| Princípio | Como o app atende |
|---|---|
| **Consentimento específico** para dados sensíveis | O questionário só é salvo com a caixa "Concordo em compartilhar estas respostas para receber orientações sobre possíveis direitos. Posso apagá-las quando quiser." marcada. A API recusa (400 em `consentimento`) se houver resposta sem o consentimento |
| **Necessidade** (só o necessário) | Todas as perguntas do questionário podem ser puladas, e há "Prefiro não informar" |
| **Finalidade** | As respostas servem só para indicar temas de direitos e ordenar o feed; não são vendidas, exportadas nem usadas em relatórios |
| **Eliminação a pedido** | "Apagar minhas respostas", no fim da tela Meus Direitos (`DELETE /usuarios/perfil`), apaga só o questionário e o rascunho dele no navegador; "Excluir minha conta" (`POST /usuarios/me/exclusao`, com a senha) apaga a conta, o questionário, as denúncias da pessoa e tudo ligado a elas, além dos votos, comentários, confirmações e reportes que ela fez |
| **Transparência** | O conteúdo de demonstração é marcado como "Exemplo", e o painel da cidade diz quantas denúncias são de exemplo |
| **Acesso e correção** | A pessoa vê e edita os dados da conta e as respostas do questionário a qualquer momento |

## 3. O que fica no navegador

| Onde | Chave | Conteúdo | Quando some |
|---|---|---|---|
| `localStorage` | `token` | Token de sessão (assinado, vale 7 dias) | Ao sair, excluir a conta ou receber 401 |
| `localStorage` | `usuario` | Nome, e-mail, cidade e UF, para mostrar sem chamar a API | Idem |
| `localStorage` | `md-novidades:<id>` | Números das próprias denúncias na última visita (para os avisos de novidades) | Só se a pessoa limpar os dados do site |
| `sessionStorage` | `md-cadastro-rascunho` | Nome, cidade, UF e e-mail digitados no cadastro (**nunca a senha**) | Ao fechar a aba ou concluir o cadastro |
| `sessionStorage` | `md-questionario-rascunho-<id>` | Respostas do questionário ainda não salvas | Ao fechar a aba ou salvar |
| `sessionStorage` | rascunho da nova denúncia, posição do feed, escolha e resultado dos canais oficiais | Conveniências da navegação | Ao fechar a aba |

Se o navegador bloquear o armazenamento (aba privada restrita), o app guarda a sessão só na memória e continua funcionando até a página ser recarregada.

## 4. Proteções da API

| Ameaça | Proteção |
|---|---|
| Alguém se passar por outra pessoa | Token HMAC-SHA256 com o id e a validade, assinado com um segredo do servidor (`token.segredo`); a assinatura é comparada em tempo constante, e cada rota protegida confere se o usuário ainda existe |
| Adivinhar senhas | 5 erros seguidos no mesmo e-mail bloqueiam o login por 15 minutos (429); hash PBKDF2 lento e com salt |
| Descobrir quais e-mails têm conta | O login responde a mesma mensagem e no mesmo tempo para e-mail inexistente e senha errada (o hash é calculado mesmo sem usuário) |
| Agir sobre o que é de outra pessoa | Editar ou excluir denúncia e comentário só pelo autor (403); confirmar resolução só moradores da mesma cidade |
| SQL injection | Todo SQL usa `PreparedStatement` com parâmetros; valores de lista são convertidos para enums antes de chegar ao banco |
| Conteúdo malicioso em imagens e links | `imagemUrl` aceita só `img/demo/<nome>.jpg|jpeg|png|webp`; o front exibe textos como texto (o React escapa o HTML) |
| Página embutida em outro site, ou o navegador "adivinhar" o tipo do conteúdo | `X-Frame-Options: DENY` e `X-Content-Type-Options: nosniff` |
| Vazamento de dados por cache ou pelo endereço de origem | `Cache-Control: no-store` nas respostas pessoais e `Referrer-Policy: no-referrer` |
| Chamadas de sites não autorizados | CORS só para as origens de `cors.origens` (o app em desenvolvimento e, para a entrega por `file://`, a origem `null`). Como a sessão vai num cabeçalho e não num cookie, liberar `null` não deixa outra página usar a sessão de alguém |
| Ofensas na conversa do bairro | Filtro de termos ofensivos e reporte pela comunidade (3 reportes ocultam o comentário) |
| Alterar o catálogo de direitos ou a demonstração | Rotas administrativas exigem `X-Chave-Admin` ou `X-Chave-Simulacao`, comparadas em tempo constante; sem chave configurada, essas rotas ficam desligadas |
| Banco fora do ar ou lento | Pool com espera máxima de 10 s; a API responde 503 em vez de travar |

## 5. Segredos e configuração

- `backend/db.properties` (fora do git) guarda o usuário e a senha do Oracle, o `token.segredo` e as chaves de administração. Também é possível usar variáveis de ambiente (`MD_DB_SENHA`, `MD_TOKEN_SEGREDO`…), o que é melhor em servidor.
- O `token.segredo` precisa ter pelo menos 32 caracteres aleatórios. Sem ele, a API gera um segredo a cada início e avisa no console: funciona, mas todas as sessões caem a cada reinício.
- Trocar o `token.segredo` invalida todos os tokens emitidos: é o jeito de "deslogar todo mundo" se um token vazar.

## 6. Limitações conhecidas

- **Sem HTTPS no protótipo.** Em produção, a API precisa ficar atrás de HTTPS; sem isso, o token e a senha trafegam em texto aberto na rede.
- **Token sem revogação individual.** Sair apaga o token do navegador, mas uma cópia continuaria válida até vencer. Só excluir a conta (ou trocar o segredo) o invalida na hora. Uma lista de tokens revogados exigiria uma tabela nova.
- **Limite de login em memória.** Zera quando a API reinicia e não seria compartilhado entre várias instâncias.
- **Filtro de termos simples.** Grafias disfarçadas passam, e um comentário oculto por reportes não tem revisão humana (enquanto oculto, ele não pode ser editado nem reportado de novo).
- **Token no `localStorage`.** Um script malicioso que rodasse na página poderia lê-lo. O risco é reduzido porque o app não usa HTML vindo de fora (o React escapa os textos) e não carrega scripts de terceiros.
- **Dados de exemplo** convivem com os reais nas mesmas tabelas, separados pela marca `st_simulado`.

## 7. Como relatar um problema de segurança

Não abra uma issue pública com detalhes de uma falha. Fale diretamente com os integrantes do grupo (contatos no [README](../README.md#integrantes)) descrevendo o problema e como reproduzi-lo.
