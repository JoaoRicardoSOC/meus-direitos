# Meus Direitos

> **FIAP · Global Solution 2026/2 · CitizenTech**
> *Sua voz melhora a cidade. Seus direitos abrem caminhos.*

Plataforma colaborativa que une **zeladoria urbana** e **informação sobre direitos sociais**:

- **Cidade:** moradores registram problemas do bairro (buracos, calçadas, iluminação, limpeza). Também votam e comentam. O feed prioriza o que mais afeta cada pessoa.
- **Meus Direitos:** a partir de um questionário opcional, a plataforma indica, em linguagem simples, possibilidades de apoio (gratuidades, atendimento prioritário, benefícios municipais, cotas) e os caminhos oficiais para confirmá-las.

**ODS atendidos:** 10 (Redução das desigualdades) e 11 (Cidades e comunidades sustentáveis).

🎥 **Vídeo pitch:** _link será adicionado na entrega_

---

## Estrutura do repositório

| Pasta | Conteúdo | Responsáveis |
|---|---|---|
| [`backend/`](backend/) | API REST em Java (JAX-RS/Jersey + Grizzly, JDBC) | João, Caio |
| [`frontend/`](frontend/) | Interface web em React + Vite | Marcela, Bielecky |
| [`database/`](database/) | `script.sql` (DDL + carga inicial) para Oracle | Vicentini |
| [`docs/`](docs/) | Diagramas (classes e ER), coleção de requisições e contrato da API | Todos |

## Tecnologias

| Camada | Tecnologias |
|---|---|
| Front-end | React, Vite, JavaScript, HTML5, CSS3, Bootstrap |
| Back-end | Java 17, Maven, JAX-RS (Jersey), Grizzly, JSON-B, JDBC, JUnit 5 |
| Banco de dados | Oracle Database |

## Como executar

1. **Banco de dados:** execute `database/script.sql` no Oracle (SQL Developer).
2. **Back-end:** siga [`backend/README.md`](backend/README.md): copie `db.properties.example` para `db.properties`, preencha usuário e senha do Oracle e rode `mvn clean compile exec:java` na pasta `backend/`. A API sobe em `http://localhost:8080/api`.
3. **Front-end:** veja `frontend/README.md`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/api.md`](docs/api.md) | Contrato da API REST: rotas, corpos, respostas, erros e guia de integração do front-end |
| [`docs/insomnia/meus-direitos.insomnia.json`](docs/insomnia/meus-direitos.insomnia.json) | Coleção do Insomnia com todas as rotas e uma pasta de erros tratados (importe em *Import → From File*) |
| [`backend/README.md`](backend/README.md) | Arquitetura, decisões técnicas e limitações do back-end |
| [`docs/diagramas/`](docs/diagramas/) | Diagrama de classes e modelo de dados |

## Uso de IA generativa

- **Back-end:** Claude (Anthropic), via Claude Code. Usado para analisar o enunciado, planejar a arquitetura, gerar e revisar código, montar os testes de robustez e documentar a API. Todo o código foi revisado, compreendido e testado pelos integrantes do back-end. Detalhes em [`backend/README.md`](backend/README.md#7-uso-de-ia-generativa-back-end).
- **Front-end:** _a completar pela equipe do front-end._
- **Banco de dados:** _a completar pelo responsável pelo banco._

## Integrantes

| Nome | RM | Frente |
|---|---|---|
| João Ricardo | _RM_ | Back-end |
| Caio | _RM_ | Back-end |
| Marcela | _RM_ | Front-end |
| Bielecky | _RM_ | Front-end |
| Vicentini | _RM_ | Banco de dados |
