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
3. **Front-end:** com a API rodando, abra **`frontend/dist/index.html`** direto no navegador (duplo clique). Para isso, inclua `null` nas origens do CORS do back-end (`cors.origens=http://localhost:5173,null` no `backend/db.properties`), porque o navegador envia a origem `null` para páginas abertas do disco. Para desenvolver, use `npm install` e `npm run dev` na pasta `frontend/` (detalhes em [`frontend/README.md`](frontend/README.md)).

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/api.md`](docs/api.md) | Contrato da API REST: rotas, corpos, respostas, erros e guia de integração do front-end |
| [`docs/insomnia/meus-direitos.insomnia.json`](docs/insomnia/meus-direitos.insomnia.json) | Coleção do Insomnia com todas as rotas e uma pasta de erros tratados (importe em *Import → From File*) |
| [`backend/README.md`](backend/README.md) | Arquitetura, decisões técnicas e limitações do back-end |
| [`frontend/README.md`](frontend/README.md) | Execução, entrega por `file://`, organização, decisões de acessibilidade e créditos das imagens do front-end |
| [`docs/diagramas/diagrama-classes.md`](docs/diagramas/diagrama-classes.md) | Diagrama de classes do back-end (domínio, camadas e erros) |
| [`docs/diagramas/diagrama-er.md`](docs/diagramas/diagrama-er.md) | Modelo de dados (diagrama ER) e regras garantidas pelo banco |
| [`database/script.sql`](database/script.sql) | Criação das tabelas no Oracle e carga inicial do catálogo de direitos |

## Integrantes

| Nome | RM | Frente |
|---|---|---|
| João Ricardo | _RM_ | Back-end |
| Caio | _RM_ | Back-end |
| Marcela | _RM_ | Front-end |
| Bielecky | _RM_ | Front-end |
| Vicentini | _RM_ | Banco de dados |
