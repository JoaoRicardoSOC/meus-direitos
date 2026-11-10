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

> Esta seção será completada ao longo do desenvolvimento.

1. **Banco de dados:** execute `database/script.sql` no Oracle (SQL Developer).
2. **Back-end:** veja `backend/README.md` (configuração da conexão e `mvn clean compile exec:java`). A API sobe em `http://localhost:8080/api`.
3. **Front-end:** veja `frontend/README.md`.

## Uso de IA generativa

> Esta seção será completada na entrega com as ferramentas usadas por cada frente e a finalidade de cada uso.

## Integrantes

| Nome | RM | Frente |
|---|---|---|
| João Ricardo | _RM_ | Back-end |
| Caio | _RM_ | Back-end |
| Marcela | _RM_ | Front-end |
| Bielecky | _RM_ | Front-end |
| Vicentini | _RM_ | Banco de dados |
