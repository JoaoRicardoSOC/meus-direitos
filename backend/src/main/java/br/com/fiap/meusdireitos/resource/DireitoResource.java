package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.config.AppConfig;
import br.com.fiap.meusdireitos.dto.request.DireitoRequest;
import br.com.fiap.meusdireitos.dto.response.DireitoResponse;
import br.com.fiap.meusdireitos.dto.response.DireitoResumoResponse;
import br.com.fiap.meusdireitos.exception.AcessoNegadoException;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.DireitoService;
import br.com.fiap.meusdireitos.service.PerfilService;
import br.com.fiap.meusdireitos.util.ChaveUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
 * Catálogo de temas de direitos (CRUD completo).
 * <p>
 * A leitura é pública. Criar, editar e excluir exigem o cabeçalho {@code X-Chave-Admin} igual a
 * {@code admin.chave} do db.properties (sem chave configurada, o catálogo fica somente leitura).
 */
@Path("direitos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DireitoResource {

    static final String CABECALHO_CHAVE = "X-Chave-Admin";

    private final DireitoService service = new DireitoService();
    private final PerfilService perfilService = new PerfilService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    @GET
    public List<DireitoResponse> listar(@QueryParam("tag") String tag) {
        return service.listar(tag);
    }

    /** Temas aplicáveis ao usuário logado, de acordo com as respostas do questionário. */
    @GET
    @Path("meus")
    public List<DireitoResumoResponse> meus(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId) {
        return perfilService.direitosDoUsuario(autenticacao.idDoUsuarioLogado(usuarioId));
    }

    @GET
    @Path("{id: \\d+}")
    public DireitoResponse buscar(@PathParam("id") Long id) {
        return service.buscar(id);
    }

    @POST
    public Response criar(@HeaderParam(CABECALHO_CHAVE) String chave, DireitoRequest req, @Context UriInfo uriInfo) {
        exigirChaveAdmin(chave);
        DireitoResponse criado = service.criar(req);
        URI local = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(local).entity(criado).build();
    }

    @PUT
    @Path("{id: \\d+}")
    public DireitoResponse atualizar(@HeaderParam(CABECALHO_CHAVE) String chave, @PathParam("id") Long id,
                                     DireitoRequest req) {
        exigirChaveAdmin(chave);
        return service.atualizar(id, req);
    }

    @DELETE
    @Path("{id: \\d+}")
    public Response excluir(@HeaderParam(CABECALHO_CHAVE) String chave, @PathParam("id") Long id) {
        exigirChaveAdmin(chave);
        service.excluir(id);
        return Response.noContent().build();
    }

    /** A escrita no catálogo é da equipe: exige o cabeçalho X-Chave-Admin igual a admin.chave. */
    private static void exigirChaveAdmin(String informada) {
        if (!ChaveUtil.confere(AppConfig.adminChave(), informada)) {
            throw new AcessoNegadoException("Somente a administração pode alterar o catálogo de direitos.");
        }
    }
}
