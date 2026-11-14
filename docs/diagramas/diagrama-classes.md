# Diagrama de classes · back-end

Gerado a partir do código em `backend/src/main/java/br/com/fiap/meusdireitos`.
O GitHub desenha os diagramas abaixo direto nesta página. As versões em imagem estão em `diagrama-classes-*.svg`, nesta mesma pasta.

O diagrama está dividido em três partes:
1. **Domínio:** as entidades e as regras que ficam nelas.
2. **Camadas:** como uma requisição atravessa resource → service → DAO → Oracle, com a autenticação por token, a moderação dos comentários e os filtros de resposta.
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
        +normalizarEmail(String email)$ String
        +isSimulado() boolean
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
        -String nomeAutor
        -boolean autorSimulado
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
        -String nomeAutor
        -boolean autorSimulado
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
        +adicionarPasso(String titulo, String texto) void
        +adicionarTag(TagDemografica tag) void
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

    class SemeaduraCidade {
        <<record>>
        String cidadeChave
        String uf
        String cidade
        LocalDateTime dataSemeadura
        LocalDateTime dataUltimoPulso
        int eventos
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
        +relevantesPara(Set~TagDemografica~ tags)$ List~CategoriaDenuncia~
    }
    class StatusDenuncia {
        <<enum>>
        ABERTA
        EM_ANDAMENTO
        RESOLVIDA
        +podeIrPara(StatusDenuncia novo) boolean
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
    Usuario "*" -- "*" Comentario : reporta
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
    CategoriaDenuncia ..> TagDemografica : relevante para
    EnumDescritivo <|.. CategoriaDenuncia
    EnumDescritivo <|.. StatusDenuncia
    EnumDescritivo <|.. TipoVoto
    EnumDescritivo <|.. TagDemografica
```

> Também implementam `EnumDescritivo`: `RacaCor`, `Deficiencia`, `PercursoEscolar`, `IconeDireito` e `OrdemFeed`.
>
> O reporte de comentário (associação "reporta") não tem classe no modelo: é só a tabela `TB_GS26S2_REPORTE_COMENTARIO` (usuário, comentário, data), acessada pelo `ReporteComentarioDao`. Com 3 reportes, o comentário deixa de aparecer na listagem e nas contagens. `SemeaduraCidade` registra as cidades que já receberam o conteúdo de demonstração ("cidade viva").

## 2. Camadas (exemplo: denúncias e comentários)

O mesmo padrão se repete para usuários, votos, direitos, perfil e relatórios: o resource cuida do HTTP, o service das regras e da transação, e o DAO do SQL.

```mermaid
classDiagram
    direction LR

    class DenunciaResource {
        <<JAX-RS @Path denuncias>>
        +feed(authorization, ordem, pagina, tamanho) PaginaResponse
        +criar(authorization, DenunciaRequest) Response
        +atualizar(authorization, id, DenunciaRequest) DenunciaResponse
        +excluir(authorization, id) Response
        +votar(authorization, id, VotoRequest) PlacarResponse
        +confirmarResolucao(authorization, id) DenunciaResponse
        +comentar(authorization, id, ComentarioRequest) Response
    }
    class ComentarioResource {
        <<JAX-RS @Path comentarios>>
        +editar(authorization, id, ComentarioRequest) ComentarioResponse
        +excluir(authorization, id) Response
        +reportar(authorization, id) Response
    }
    class AuthResource {
        <<JAX-RS @Path auth>>
        +entrar(LoginRequest) UsuarioResponse
    }
    class MetadadosResource {
        <<JAX-RS @Path raiz>>
        ~CACHE_LISTAS_FIXAS = "public, max-age=86400"
        +categorias() Response
        +statusDenuncia() Response
        +racasCor() Response
        +deficiencias() Response
        +percursosEscolares() Response
    }

    class AutenticacaoService {
        -LimiteTentativasLogin LIMITE$
        -String HASH_FICTICIO$
        -Predicate~Long~ usuarioExiste
        +entrar(LoginRequest req) UsuarioResponse
        +idDoUsuarioLogado(String cabecalho) Long
        +idOpcional(String cabecalho) Long
        ~extrairBearer(String cabecalho)$ String
    }
    class LimiteTentativasLogin {
        ~MAXIMO_FALHAS = 5
        ~BLOQUEIO = 15 min
        -ConcurrentMap~String, Situacao~ situacoes
        +verificar(String email) void
        +registrarFalha(String email) void
        +registrarSucesso(String email) void
    }
    class TokenUtil {
        <<final>>
        +VALIDADE = 7 dias
        +gerar(long idUsuario)$ String
        +validar(String token)$ Long
    }
    class DenunciaService {
        +criar(Long idUsuario, DenunciaRequest req) DenunciaResponse
        +atualizar(Long idUsuario, Long id, DenunciaRequest req) DenunciaResponse
        +excluir(Long idUsuario, Long id) void
    }
    class FeedService {
        ~BONUS_RELEVANCIA = 10
        ~PENALIDADE_RESOLVIDA = 15
        +feed(Long idUsuario, String ordem, String pagina, String tamanho) PaginaResponse
    }
    class ComentarioService {
        +REPORTES_PARA_OCULTAR = 3$
        +listar(Long idDenuncia, Long idLeitor) List~ComentarioResponse~
        +criar(Long idUsuario, Long idDenuncia, ComentarioRequest req) ComentarioResponse
        +editar(Long idUsuario, Long idComentario, ComentarioRequest req) ComentarioResponse
        +excluir(Long idUsuario, Long idComentario) void
        +reportar(Long idUsuario, Long idComentario) void
    }
    class FiltroTermosOfensivos {
        <<final>>
        ~Set~String~ TERMOS$
        +contemTermoOfensivo(String texto)$ boolean
    }
    class Validador {
        +obrigatorio(campo, valor) Validador
        +tamanho(campo, valor, min, max) Validador
        +imagem(campo, valor) Validador
        +regra(condicao, campo, mensagem) Validador
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
        +emprestar()$ Connection
        +devolver(Connection conexao)$ void
        ~tamanhoDoPool(String valor)$ int
    }
    class PoolConexoes {
        <<final>>
        -BlockingQueue~Connection~ livres
        -Semaphore vagas
        -Set~Connection~ emprestadas
        ~emprestar() Connection
        ~devolver(Connection conexao) void
        ~fecharLivres() void
    }
    class AppConfig {
        <<final>>
        +dbUrl()$ String
        +corsOrigens()$ List~String~
        +adminChave()$ String
        +tokenSegredo()$ String
        +pendencias()$ List~String~
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
    class ComentarioDao {
        <<interface>>
        +buscarVisivelPorId(Long id) Comentario
        +listarPorDenuncia(Long idDenuncia) List~Comentario~
        +excluirPorDenuncia(Long idDenuncia) void
        +excluirDoUsuario(Long idUsuario) void
    }
    class ReporteComentarioDao {
        <<interface>>
        +reportar(Long idUsuario, Long idComentario) boolean
        +contar(Long idComentario) int
        +excluirPorComentario(Long idComentario) void
        +excluirPorDenuncia(Long idDenuncia) void
        +excluirDoUsuario(Long idUsuario) void
    }
    class DenunciaDaoJdbc {
        -Connection conexao
    }
    class ComentarioDaoJdbc {
        -Connection conexao
    }
    class ReporteComentarioDaoJdbc {
        -Connection conexao
        +naoOculto(String colunaComentario)$ String
    }

    class ContainerResponseFilter {
        <<interface JAX-RS>>
        +filter(requisicao, resposta) void
    }
    class CorsFilter
    class CabecalhosSegurancaFilter {
        ~SEM_CACHE = "no-store"
    }

    class PerfilRequest {
        <<record>>
        LocalDate dataNascimento
        RacaCor racaCor
        Deficiencia deficiencia
        PercursoEscolar percursoEscolar
        Boolean baixaRenda
        Boolean consentimento
        +temDadoSensivel() boolean
    }
    class PerfilService {
        ~MSG_SEM_CONSENTIMENTO
        +salvar(Long idUsuario, PerfilRequest req) PerfilCompletoResponse
        ~validar(PerfilRequest req, LocalDate hoje)$ void
    }
    class UsuarioResponse {
        <<record>>
        Long id
        String nome
        String email
        String cidade
        String uf
        LocalDateTime criadoEm
        boolean perfilPreenchido
        Boolean cidadePreparada
        String token
        +comCidadePreparada(Boolean preparada) UsuarioResponse
        +comToken(String novoToken) UsuarioResponse
    }

    DenunciaResource --> AutenticacaoService
    DenunciaResource --> DenunciaService
    DenunciaResource --> FeedService
    DenunciaResource --> ComentarioService
    ComentarioResource --> AutenticacaoService
    ComentarioResource --> ComentarioService
    AuthResource --> AutenticacaoService
    AutenticacaoService --> LimiteTentativasLogin
    AutenticacaoService ..> TokenUtil
    AutenticacaoService ..> UsuarioResponse : devolve com token
    TokenUtil ..> AppConfig : segredo
    DenunciaService ..> Validador
    DenunciaService ..> Transacao
    FeedService ..> Transacao
    ComentarioService ..> Validador
    ComentarioService ..> FiltroTermosOfensivos
    ComentarioService ..> Transacao
    DenunciaService ..> DenunciaDao
    DenunciaService ..> ReporteComentarioDao
    ComentarioService ..> ComentarioDao
    ComentarioService ..> ReporteComentarioDao
    PerfilService ..> PerfilRequest
    PerfilService ..> Validador
    Transacao ..> ConnectionFactory
    ConnectionFactory --> PoolConexoes
    ConnectionFactory ..> AppConfig
    CrudDao <|-- DenunciaDao
    CrudDao <|-- ComentarioDao
    DenunciaDao <|.. DenunciaDaoJdbc
    ComentarioDao <|.. ComentarioDaoJdbc
    ReporteComentarioDao <|.. ReporteComentarioDaoJdbc
    ComentarioDaoJdbc ..> ReporteComentarioDaoJdbc : naoOculto
    DenunciaDaoJdbc ..> ReporteComentarioDaoJdbc : naoOculto
    ContainerResponseFilter <|.. CorsFilter
    ContainerResponseFilter <|.. CabecalhosSegurancaFilter
    CorsFilter ..> AppConfig
    CabecalhosSegurancaFilter ..> MetadadosResource : respeita o Cache-Control
```

Como ler:
- **Autenticação.** Toda rota 🔒 chama `AutenticacaoService.idDoUsuarioLogado(Authorization)`: tira o `Bearer`, valida o token no `TokenUtil` (HMAC-SHA256 com `AppConfig.tokenSegredo()`, validade de 7 dias) e confere se o usuário ainda existe. Rotas públicas usam `idOpcional`, que nunca lança erro (token inválido ou de conta excluída vira visitante). O login passa antes pelo `LimiteTentativasLogin` (5 falhas seguidas no mesmo e-mail bloqueiam por 15 minutos, HTTP 429) e devolve o `UsuarioResponse` com `comToken(...)`.
- **Moderação.** `ComentarioService` recusa texto com termo ofensivo (`FiltroTermosOfensivos`, 400 em `texto`) e grava reportes pelo `ReporteComentarioDao`. Os DAOs que listam e contam comentários usam `ReporteComentarioDaoJdbc.naoOculto(...)`, a condição SQL "menos de 3 reportes"; editar e reportar buscam o comentário com `buscarVisivelPorId`, que aplica a mesma condição (oculto = 404). Ao excluir comentário, denúncia ou conta, os reportes ligados são apagados antes, na mesma transação.
- **Consentimento.** `PerfilRequest.temDadoSensivel()` diz se veio algum dado sensível; nesse caso `PerfilService.validar` exige `consentimento == true` (400 no campo `consentimento`).
- **Conexões.** `Transacao` pede a conexão à `ConnectionFactory`, que a empresta do `PoolConexoes` (até `MD_DB_POOL` conexões, padrão 5). A `Transacao` devolve a conexão com `ConnectionFactory.devolver` num bloco `finally`.
- **Filtros de resposta.** `CorsFilter` e `CabecalhosSegurancaFilter` implementam `ContainerResponseFilter` e agem em toda resposta. O segundo põe `Cache-Control: no-store`, exceto quando a rota já definiu o seu, como as listas fixas do `MetadadosResource` (`public, max-age=86400`).

> DAOs que estendem `CrudDao<T>`: `UsuarioDao`, `DenunciaDao`, `ComentarioDao` e `DireitoDao`. Os demais (`VotoDenunciaDao`, `ConfirmacaoResolucaoDao`, `ReporteComentarioDao`, `PerfilDemograficoDao`, `RelatorioDao`, `SemeaduraCidadeDao`) têm operações próprias. Cada um tem a sua implementação `*DaoJdbc`, que recebe a `Connection` aberta pela `Transacao`.

## 3. Erros (`exception`)

```mermaid
classDiagram
    direction TB

    class MeusDireitosException {
        <<abstract>>
        +getStatusHttp()* int
        +getTitulo()* String
        +getCampos() List~ErroCampo~
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
    class MuitasTentativasException {
        429
    }
    class PersistenciaException {
        -boolean bancoIndisponivel
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
    MeusDireitosException <|-- MuitasTentativasException
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
    class RespostaErro {
        <<final>>
        ~montar(status, erro, mensagem, campos, uriInfo)$ Response
    }
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
    MeusDireitosExceptionMapper ..> MeusDireitosException : trata
    MeusDireitosExceptionMapper ..> RespostaErro
    WebApplicationExceptionMapper ..> RespostaErro
    JsonInvalidoMapper ..> RespostaErro
    ErroInesperadoMapper ..> RespostaErro
    RespostaErro ..> ErroResponse : monta
```

O `getStatusHttp()` é polimórfico: o `MeusDireitosExceptionMapper` não tem nenhum `if`. Cada exceção sabe o seu status. Os quatro mappers usam o mesmo `RespostaErro.montar(...)`, que preenche o `ErroResponse` com a rota chamada (`caminho`) e o momento do erro.

| Mapper | Trata | Status |
|---|---|---|
| `MeusDireitosExceptionMapper` | Exceções da aplicação | O da exceção (400, 401, 403, 404, 409, 422, 429, 500 ou 503) |
| `JsonInvalidoMapper` | JSON malformado, enum inexistente, data inválida (`ProcessingException` do JSON-B) | 400 |
| `WebApplicationExceptionMapper` | Erros do Jersey: rota inexistente, método ou `Content-Type` errado | O original (404, 405, 415…), mantendo o cabeçalho `Allow` |
| `ErroInesperadoMapper` | Qualquer outro `Throwable` | 500, sem stack trace na resposta |

A `MuitasTentativasException` (429) é lançada pelo `LimiteTentativasLogin` quando o e-mail está bloqueado.

O `PersistenciaException.de` traduz o código do Oracle para a exceção certa:

| Código do Oracle | Significado | Exceção (status) |
|---|---|---|
| ORA-00001 | registro duplicado | `ConflitoException` (409) |
| ORA-02292 | registro com vínculos | `ConflitoException` (409) |
| ORA-02291 | registro relacionado inexistente | `EntidadeNaoEncontradaException` (404) |
| ORA-01400, ORA-02290, ORA-12899 | obrigatório, valor não permitido, valor grande demais | `ValidacaoException` (400) |
| Erro de conexão (ORA-12541, ORA-12514, ORA-12505, ORA-01017, ORA-28000, ORA-17002, SQLState `08…`) | banco fora do ar | `PersistenciaException` (503) |
| Pool sem conexão livre depois de 10 s (SQLState `08001`) | todas as conexões em uso | `PersistenciaException` (503) |
| Outros | — | `PersistenciaException` (500) |
