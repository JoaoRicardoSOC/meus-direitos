package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.AtualizacaoUsuarioRequest;
import br.com.fiap.meusdireitos.dto.request.CadastroUsuarioRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioPublicoResponse;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.UsuarioService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

/**
 * Endpoints de usuários: cadastro, conta do usuário logado e consulta pública.
 */
@Path("usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    /** Cabeçalho com o id do usuário logado (enviado pelo front-end após o login). */
    public static final String CABECALHO_USUARIO = "X-Usuario-Id";

    private final UsuarioService service = new UsuarioService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    @POST
    public Response cadastrar(CadastroUsuarioRequest req, @Context UriInfo uriInfo) {
        UsuarioResponse criado = service.cadastrar(req);
        URI local = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(local).entity(criado).build();
    }

    @GET
    public List<UsuarioPublicoResponse> listar() {
        return service.listarPublico();
    }

    @GET
    @Path("{id: \\d+}")
    public UsuarioPublicoResponse buscar(@PathParam("id") Long id) {
        return service.buscarPublico(id);
    }

    @GET
    @Path("me")
    public UsuarioResponse minhaConta(@HeaderParam(CABECALHO_USUARIO) String usuarioId) {
        return service.buscarConta(autenticacao.idDoUsuarioLogado(usuarioId));
    }

    @PUT
    @Path("me")
    public UsuarioResponse atualizarMinhaConta(@HeaderParam(CABECALHO_USUARIO) String usuarioId,
                                               AtualizacaoUsuarioRequest req) {
        return service.atualizarConta(autenticacao.idDoUsuarioLogado(usuarioId), req);
    }
}
