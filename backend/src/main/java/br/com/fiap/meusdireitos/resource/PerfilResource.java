package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.PerfilRequest;
import br.com.fiap.meusdireitos.dto.response.PerfilCompletoResponse;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.PerfilService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Questionário "Meus Direitos" do usuário logado (dados sensíveis: só o próprio usuário acessa).
 */
@Path("usuarios/perfil")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PerfilResource {

    private final PerfilService service = new PerfilService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    @GET
    public PerfilCompletoResponse obter(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId) {
        return service.obter(autenticacao.idDoUsuarioLogado(usuarioId));
    }

    @PUT
    public PerfilCompletoResponse salvar(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId,
                                         PerfilRequest req) {
        return service.salvar(autenticacao.idDoUsuarioLogado(usuarioId), req);
    }

    @DELETE
    public Response excluir(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId) {
        service.excluir(autenticacao.idDoUsuarioLogado(usuarioId));
        return Response.noContent().build();
    }
}
