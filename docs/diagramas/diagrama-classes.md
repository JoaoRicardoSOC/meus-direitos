# Diagrama de classes · back-end

Gerado a partir do código em `backend/src/main/java/br/com/fiap/meusdireitos`.
O GitHub desenha os diagramas abaixo direto nesta página. As versões em imagem estão em `diagrama-classes-*.svg`, nesta mesma pasta.

O diagrama está dividido em três partes:
1. **Domínio:** as entidades e as regras que ficam nelas.
2. **Camadas:** como uma requisição atravessa resource → service → DAO → Oracle.
3. **Erros:** a hierarquia de exceções e os mappers que garantem a resposta padronizada.

Getters e setters foram omitidos.

## 1. Domínio (`model`)

```mermaid
classDiagram
    direction LR

    class EntidadeBase {
        <<abstract>>
        -Long id
        +isNova() boolean
    }

    class Usuario {
        -String nome
        -String email
        -String senhaHash
        -String cidade
        -String uf
        -LocalDateTime dataCadastro
        -boolean simulado
    }

    class PerfilDemografico {
        -Long idUsuario
        -LocalDate dataNascimento
        -RacaCor racaCor
        -Deficiencia deficiencia
        -PercursoEscolar percursoEscolar
        -Boolean baixaRenda
        -LocalDateTime dataAtualizacao
        +calcularIdade(LocalDate hoje) Integer
        +calcularTags(LocalDate hoje) Set~TagDemografica~
    }

    class Denuncia {
        +int CONFIRMACOES_PARA_RESOLVER = 3
        -Long idAutor
        -String titulo
        -String descricao
        -CategoriaDenuncia categoria
        -StatusDenuncia status
        -String endereco
        -String bairro
        -String cidade
        -String uf
        -String imagemUrl
        -LocalDateTime dataCriacao
        -LocalDateTime dataAtualizacao
        +getProtocolo() String
        +isAutor(Long idUsuario) boolean
        +ehDaCidade(String cidade, String uf) boolean
        +aceitaConfirmacaoDeResolucao() boolean
        +confirmacoesSuficientes(int qtd)$ boolean
    }

    class Comentario {
        -Long idDenuncia
        -Long idAutor
        -String texto
        -LocalDateTime dataCriacao
        -LocalDateTime dataEdicao
        +isAutor(Long idUsuario) boolean
    }

    class VotoDenuncia {
        -Long idUsuario
        -Long idDenuncia
        -TipoVoto tipo
        -LocalDateTime dataVoto
    }

    class ConfirmacaoResolucao {
        <<record>>
        Long idUsuario
        Long idDenuncia
        LocalDateTime data
    }

    class Direito {
        -String titulo
        -String resumo
        -String descricao
        -String aviso
        -String baseLegal
        -String linkOficial
        -IconeDireito icone
        -int ordem
        -List~PassoDireito~ passos
        -Set~TagDemografica~ tags
    }

    class PassoDireito {
        <<record>>
        int ordem
        String titulo
        String texto
    }

    class Placar {
        <<record>>
        int upvotes
        int downvotes
        TipoVoto meuVoto
        +saldo() int
    }

    class DenunciaDetalhada {
        -Denuncia denuncia
        -Placar placar
        -int comentarios
        -int confirmacoesResolucao
        -boolean confirmadaPeloLeitor
        -Boolean relevanteParaLeitor
    }

    class EnumDescritivo {
        <<interface>>
        +getDescricao() String
    }
    class CategoriaDenuncia {
        <<enum>>
        ASFALTO
        ILUMINACAO
        CALCADA_ACESSIBILIDADE
        SEGURANCA
        LIMPEZA_URBANA
        OUTROS
    }
    class StatusDenuncia {
        <<enum>>
        ABERTA
        EM_ANDAMENTO
        RESOLVIDA
    }
    class TipoVoto {
        <<enum>>
        UP
        DOWN
    }
    class TagDemografica {
        <<enum>>
        IDOSO
        JOVEM
        PCD
        PCD_MOBILIDADE
        ...
    }

    EntidadeBase <|-- Usuario
    EntidadeBase <|-- Denuncia
    EntidadeBase <|-- Comentario
    EntidadeBase <|-- Direito

    Usuario "1" -- "0..1" PerfilDemografico : responde
    Usuario "1" -- "*" Denuncia : registra
    Usuario "1" -- "*" Comentario : escreve
    Usuario "1" -- "*" VotoDenuncia : vota
    Usuario "1" -- "*" ConfirmacaoResolucao : confirma
    Denuncia "1" *-- "*" Comentario
    Denuncia "1" *-- "*" VotoDenuncia
    Denuncia "1" *-- "*" ConfirmacaoResolucao
    Direito "1" *-- "*" PassoDireito
    Direito "*" -- "*" TagDemografica : aplica-se a
    PerfilDemografico ..> TagDemografica : calcula
    DenunciaDetalhada o-- Denuncia
    DenunciaDetalhada o-- Placar
    Denuncia --> CategoriaDenuncia
    Denuncia --> StatusDenuncia
    VotoDenuncia --> TipoVoto
    EnumDescritivo <|.. CategoriaDenuncia
    EnumDescritivo <|.. StatusDenuncia
    EnumDescritivo <|.. TipoVoto
    EnumDescritivo <|.. TagDemografica
```

> Também implementam `EnumDescritivo`: `RacaCor`, `Deficiencia`, `PercursoEscolar`, `IconeDireito` e `OrdemFeed`.

## 2. Camadas (exemplo: denúncias)

O mesmo padrão se repete para usuários, comentários, votos, direitos, perfil e relatórios.

```mermaid
classDiagram
    direction TB

    class DenunciaResource {
        <<JAX-RS @Path denuncias>>
        +feed(usuarioId, ordem, pagina, tamanho) PaginaResponse
        +criar(usuarioId, DenunciaRequest) Response
        +atualizar(usuarioId, id, DenunciaRequest) DenunciaResponse
        +excluir(usuarioId, id) Response
        +votar(usuarioId, id, VotoRequest) PlacarResponse
        +confirmarResolucao(usuarioId, id) DenunciaResponse
    }

    class AutenticacaoService {
        +idDoUsuarioLogado(String cabecalho) Long
        +idOpcional(String cabecalho) Long
    }
    class DenunciaService {
        +criar(Long idUsuario, DenunciaRequest req) DenunciaResponse
        +atualizar(Long idUsuario, Long id, DenunciaRequest req) DenunciaResponse
        +excluir(Long idUsuario, Long id) void
    }
    class FeedService {
        +BONUS_RELEVANCIA = 10
        +PENALIDADE_RESOLVIDA = 15
        +feed(Long idUsuario, String ordem, String pagina, String tamanho) PaginaResponse
    }
    class Validador {
        +obrigatorio(campo, valor) Validador
        +tamanho(campo, valor, min, max) Validador
        +imagem(campo, valor) Validador
        +lancarSeHouverErros() void
    }

    class Transacao {
        <<final>>
        +executar(OperacaoComConexao~R~)$ R
        +executarSemRetorno(AcaoComConexao)$ void
        +consultar(OperacaoComConexao~R~)$ R
    }
    class ConnectionFactory {
        <<final>>
        +getConnection()$ Connection
    }

    class CrudDao~T~ {
        <<interface>>
        +inserir(T) T
        +buscarPorId(Long) T
        +listar() List~T~
        +atualizar(T) void
        +excluir(Long) void
    }
    class DenunciaDao {
        <<interface>>
        +feed(...) Pagina~DenunciaDetalhada~
        +pesquisar(FiltroDenuncia, ...) Pagina~DenunciaDetalhada~
        +buscarDetalhada(Long id, Long idLeitor) DenunciaDetalhada
        +inserirEmLote(List~Denuncia~) void
    }
    class DenunciaDaoJdbc {
        -Connection conexao
    }
    class AppConfig {
        <<final>>
        +dbUrl()$ String
        +corsOrigens()$ List~String~
        +adminChave()$ String
    }

    DenunciaResource --> AutenticacaoService
    DenunciaResource --> DenunciaService
    DenunciaResource --> FeedService
    DenunciaService ..> Validador
    DenunciaService ..> Transacao
    FeedService ..> Transacao
    DenunciaService ..> DenunciaDao
    Transacao ..> ConnectionFactory
    ConnectionFactory ..> AppConfig
    CrudDao <|-- DenunciaDao
    DenunciaDao <|.. DenunciaDaoJdbc
```

> DAOs que estendem `CrudDao<T>`: `UsuarioDao`, `DenunciaDao`, `ComentarioDao` e `DireitoDao`. Os demais (`VotoDenunciaDao`, `ConfirmacaoResolucaoDao`, `PerfilDemograficoDao`, `RelatorioDao`, `SemeaduraCidadeDao`) têm operações próprias. Cada um tem a sua implementação `*DaoJdbc`, que recebe a `Connection` aberta pela `Transacao`.

## 3. Erros (`exception`)

```mermaid
classDiagram
    direction TB

    class MeusDireitosException {
        <<abstract>>
        +getStatusHttp()* int
        +getTitulo()* String
    }
    class ValidacaoException {
        -List~ErroCampo~ campos
        400
    }
    class NaoAutenticadoException {
        401
    }
    class AcessoNegadoException {
        403
    }
    class EntidadeNaoEncontradaException {
        404
    }
    class ConflitoException {
        409
    }
    class RegraNegocioException {
        422
    }
    class PersistenciaException {
        503 ou 500
        +de(SQLException)$ MeusDireitosException
    }
    class ErroCampo {
        <<record>>
        String campo
        String mensagem
    }

    RuntimeException <|-- MeusDireitosException
    MeusDireitosException <|-- ValidacaoException
    MeusDireitosException <|-- NaoAutenticadoException
    MeusDireitosException <|-- AcessoNegadoException
    MeusDireitosException <|-- EntidadeNaoEncontradaException
    MeusDireitosException <|-- ConflitoException
    MeusDireitosException <|-- RegraNegocioException
    MeusDireitosException <|-- PersistenciaException
    ValidacaoException o-- ErroCampo

    class ExceptionMapper~E~ {
        <<interface JAX-RS>>
        +toResponse(E) Response
    }
    class MeusDireitosExceptionMapper
    class WebApplicationExceptionMapper
    class JsonInvalidoMapper
    class ErroInesperadoMapper
    class ErroResponse {
        <<record>>
        int status
        String erro
        String mensagem
        List~ErroCampo~ campos
        String caminho
        LocalDateTime timestamp
    }

    ExceptionMapper <|.. MeusDireitosExceptionMapper
    ExceptionMapper <|.. WebApplicationExceptionMapper
    ExceptionMapper <|.. JsonInvalidoMapper
    ExceptionMapper <|.. ErroInesperadoMapper
    MeusDireitosExceptionMapper ..> ErroResponse
    WebApplicationExceptionMapper ..> ErroResponse
    JsonInvalidoMapper ..> ErroResponse
    ErroInesperadoMapper ..> ErroResponse
```

O `getStatusHttp()` é polimórfico: o `MeusDireitosExceptionMapper` não tem nenhum `if`. Cada exceção sabe o seu status.

O `PersistenciaException.de` traduz o código do Oracle para a exceção certa:

| Código do Oracle | Significado | Exceção (status) |
|---|---|---|
| ORA-00001 | registro duplicado | `ConflitoException` (409) |
| ORA-02292 | registro com vínculos | `ConflitoException` (409) |
| ORA-02291 | registro relacionado inexistente | `EntidadeNaoEncontradaException` (404) |
| ORA-01400, ORA-02290, ORA-12899 | obrigatório, valor não permitido, valor grande demais | `ValidacaoException` (400) |
| Erro de conexão (ORA-12541, ORA-12514, ORA-01017…) | banco fora do ar | `PersistenciaException` (503) |
| Outros | — | `PersistenciaException` (500) |
