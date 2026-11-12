# Roteiro de demonstração

Passo a passo para gravar o vídeo pitch ou apresentar o projeto ao vivo, mostrando cada parte do produto na ordem em que ela faz sentido. Os tempos somam cerca de 4 minutos; ajuste ao limite pedido na entrega.

## 1. Preparação (antes de gravar)

- [ ] `backend/db.properties` com usuário, senha, `token.segredo` e `cors.origens=http://localhost:5173,null`.
- [ ] API no ar: `mvn clean compile exec:java` na pasta `backend/` e `GET http://localhost:8080/api/health` respondendo `"banco":"UP"`.
- [ ] App aberto pelo **`frontend/dist/index.html`** (duplo clique), como na entrega. Para gravar em tamanho de celular, use as ferramentas do navegador (F12 → modo dispositivo, 390 × 844); em tela larga o app já aparece numa moldura de celular com o painel de apresentação ao lado.
- [ ] Cidade escolhida para a demonstração com o conteúdo de exemplo já pronto. Se quiser evitar a espera de 1 a 2 s no cadastro, prepare antes com `POST /api/simulacao/cidades` (pasta "09 · Demonstração" do Insomnia).
- [ ] Uma conta criada **antes** para mostrar o login, e os dados de uma conta nova para mostrar o cadastro (e-mail que ainda não exista).
- [ ] Permissão de localização do navegador liberada para o arquivo, se for mostrar "Usar minha localização" e os canais oficiais.
- [ ] Notificações do computador desligadas e abas pessoais fechadas.

## 2. Roteiro

| Tempo | Tela | O que fazer | O que dizer |
|---|---|---|---|
| 0:00 | Boas-vindas | Mostrar a tela inicial | O problema: problemas do bairro se perdem, e muita gente não sabe quais direitos tem. O Meus Direitos junta as duas coisas (ODS 10 e 11) |
| 0:20 | Cadastro (4 etapas) | Preencher nome, cidade e UF, e-mail e senha. Mostrar um erro de validação (senha sem número) | Validação em JavaScript com as mesmas regras da API; o voltar do celular volta uma etapa |
| 0:45 | Feed da cidade | Rolar o feed; trocar entre "Para você", "Recentes" e "Mais votados"; mostrar o selo "Exemplo" | A cidade já começa com conteúdo de exemplo, sempre identificado. "Para você" ordena pelo que mais afeta a pessoa |
| 1:05 | Feed: busca e filtros | Buscar "buraco" e filtrar por categoria | Busca sem diferenciar acentos ("sao paulo" encontra "São Paulo") |
| 1:15 | Card | Tocar em "Apoiar" | O voto aparece na hora e a API confirma em seguida |
| 1:25 | Detalhe da denúncia | Abrir uma denúncia, comentar na conversa do bairro, mostrar "Denunciar" num comentário de outra pessoa | Moderação: palavrões são recusados e 3 denúncias escondem um comentário |
| 1:45 | Resolução pela comunidade | Numa denúncia com "2 de 3 moradores confirmaram", tocar em "Já foi resolvido" | Na terceira confirmação de moradores diferentes, a denúncia vira Resolvida sozinha |
| 2:00 | Nova denúncia | Escolher categoria, foto de demonstração, "Usar minha localização" para o endereço, publicar | A denúncia fica na cidade da pessoa e recebe um protocolo |
| 2:30 | Painel da cidade | "Ver painel completo" no feed | Os números da cidade: por categoria, bairros com mais problemas e o que mais engaja |
| 2:50 | Meus Direitos | Responder o questionário (data de nascimento 60+, deficiência física), marcar o consentimento, ver os temas | Perguntas opcionais; os dados sensíveis só são salvos com consentimento e nunca aparecem para outras pessoas |
| 3:20 | Tema e canais oficiais | Abrir um tema; em "Canais oficiais", usar a localização atual | O app não promete o direito: mostra os passos e onde confirmar (prefeitura da cidade, Gov.br e CRAS mais próximos) |
| 3:40 | Perfil e Minhas denúncias | Mostrar a denúncia criada e, se já houver apoios ou comentários nela, o aviso de novidades | A pessoa acompanha o que aconteceu desde a última visita |
| 3:55 | Encerramento | Voltar ao feed | Tecnologias: Java com JAX-RS e JDBC, Oracle e React, com acessibilidade em todas as telas |

## 3. Se algo der errado

| Problema | Saída |
|---|---|
| A API não responde | Conferir o `GET /api/health`; o app mostra uma mensagem de falta de conexão. Ter uma gravação de reserva |
| Login bloqueado (429) | É o limite de 5 tentativas: use outra conta ou reinicie a API (o bloqueio fica em memória) |
| A localização não funciona | Usar "Cidade do cadastro" nos canais oficiais e digitar o endereço da denúncia |
| O feed demora na primeira conta da cidade | É a preparação da "cidade viva" (1 a 2 s). Prepare a cidade antes (seção 1) |

## 4. Depois da gravação

- Apague as contas de teste pelo próprio app (Perfil → Excluir minha conta).
- Se quiser limpar todo o conteúdo de exemplo: `DELETE /api/simulacao` com o cabeçalho `X-Chave-Simulacao` (ver o [guia do banco](banco-de-dados.md#5-conteúdo-de-demonstração-cidade-viva)).
- Coloque o link público do vídeo no [`integrantes.txt`](../integrantes.txt) e no [README](../README.md).
