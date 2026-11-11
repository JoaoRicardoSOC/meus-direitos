package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.ComentarioRequest;
import br.com.fiap.meusdireitos.dto.request.DenunciaRequest;
import br.com.fiap.meusdireitos.dto.request.StatusRequest;
import br.com.fiap.meusdireitos.dto.request.VotoRequest;
import br.com.fiap.meusdireitos.dto.response.ComentarioResponse;
import br.com.fiap.meusdireitos.dto.response.DenunciaResponse;
import br.com.fiap.meusdireitos.dto.response.PaginaResponse;
import br.com.fiap.meusdireitos.dto.response.PlacarResponse;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.ComentarioService;
import br.com.fiap.meusdireitos.service.DenunciaService;
import br.com.fiap.meusdireitos.service.FeedService;
import br.com.fiap.meusdireitos.service.VotoService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

/**
 * Denúncias de zeladoria: feed da cidade, CRUD, status, votos e "Conversa do bairro".
 */
@Path("denuncias")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DenunciaResource {

    private static final String USUARIO = UsuarioResource.CABECALHO_USUARIO;

    private final DenunciaService denuncias = new DenunciaService();
    private final FeedService feed = new FeedService();
    private final VotoService votos = new VotoService();
    private final ComentarioService comentarios = new ComentarioService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    // ------------------------------------------------------------------ consultas

    /** Feed da cidade do usuário: ordem = PARA_VOCE (padrão), RECENTES ou MAIS_VOTADOS. */
    @GET
    @Path("feed")
    public PaginaResponse<DenunciaResponse> feed(@HeaderParam(USUARIO) String usuarioId,
                                                 @QueryParam("ordem") String ordem,
                                                 @QueryParam("pagina") String pagina,
                                                 @QueryParam("tamanho") String tamanho) {
        return feed.feed(autenticacao.idDoUsuarioLogado(usuarioId), ordem, pagina, tamanho);
    }

    /** Listagem pública com filtros opcionais. */
    @GET
    public PaginaResponse<DenunciaResponse> pesquisar(@HeaderParam(USUARIO) String usuarioId,
                                                      @QueryParam("cidade") String cidade,
                                                      @QueryParam("uf") String uf,
                                                      @QueryParam("categoria") String categoria,
                                                      @QueryParam("status") String status,
                                                      @QueryParam("busca") String busca,
                                                      @QueryParam("pagina") String pagina,
                                                      @QueryParam("tamanho") String tamanho) {
        return denuncias.pesquisar(cidade, uf, categoria, status, busca, pagina, tamanho,
                autenticacao.idOpcional(usuarioId));
    }

    @GET
    @Path("minhas")
    public List<DenunciaResponse> minhas(@HeaderParam(USUARIO) String usuarioId) {
        return denuncias.minhas(autenticacao.idDoUsuarioLogado(usuarioId));
    }

    @GET
    @Path("{id: \\d+}")
    public DenunciaResponse buscar(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id) {
        return denuncias.buscar(id, autenticacao.idOpcional(usuarioId));
    }

    // ------------------------------------------------------------------ CRUD do autor

    @POST
    public Response criar(@HeaderParam(USUARIO) String usuarioId, DenunciaRequest req, @Context UriInfo uriInfo) {
        DenunciaResponse criada = denuncias.criar(autenticacao.idDoUsuarioLogado(usuarioId), req);
        URI local = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criada.id())).build();
        return Response.created(local).entity(criada).build();
    }

    @PUT
    @Path("{id: \\d+}")
    public DenunciaResponse atualizar(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id,
                                      DenunciaRequest req) {
        return denuncias.atualizar(autenticacao.idDoUsuarioLogado(usuarioId), id, req);
    }

    @PATCH
    @Path("{id: \\d+}/status")
    public DenunciaResponse alterarStatus(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id,
                                          StatusRequest req) {
        return denuncias.alterarStatus(autenticacao.idDoUsuarioLogado(usuarioId), id, req);
    }

    @DELETE
    @Path("{id: \\d+}")
    public Response excluir(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id) {
        denuncias.excluir(autenticacao.idDoUsuarioLogado(usuarioId), id);
        return Response.noContent().build();
    }

    // ------------------------------------------------------------------ votos

    @PUT
    @Path("{id: \\d+}/voto")
    public PlacarResponse votar(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id, VotoRequest req) {
        return votos.votar(autenticacao.idDoUsuarioLogado(usuarioId), id, req);
    }

    @DELETE
    @Path("{id: \\d+}/voto")
    public PlacarResponse removerVoto(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id) {
        return votos.removerVoto(autenticacao.idDoUsuarioLogado(usuarioId), id);
    }

    // ------------------------------------------------------------------ comentários

    @GET
    @Path("{id: \\d+}/comentarios")
    public List<ComentarioResponse> comentarios(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id) {
        return comentarios.listar(id, autenticacao.idOpcional(usuarioId));
    }

    @POST
    @Path("{id: \\d+}/comentarios")
    public Response comentar(@HeaderParam(USUARIO) String usuarioId, @PathParam("id") Long id,
                             ComentarioRequest req, @Context UriInfo uriInfo) {
        ComentarioResponse criado = comentarios.criar(autenticacao.idDoUsuarioLogado(usuarioId), id, req);
        URI local = uriInfo.getBaseUriBuilder().path("comentarios").path(String.valueOf(criado.id())).build();
        return Response.created(local).entity(criado).build();
    }
}
