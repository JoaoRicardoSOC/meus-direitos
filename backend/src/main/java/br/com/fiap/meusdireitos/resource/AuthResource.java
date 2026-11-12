package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.LoginRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Login por e-mail e senha. A resposta traz o token de sessão ({@code token}), que o front envia
 * em {@code Authorization: Bearer <token>} nas rotas protegidas. Após 5 falhas seguidas no mesmo
 * e-mail, o login fica bloqueado por 15 minutos (HTTP 429).
 */
@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AutenticacaoService service = new AutenticacaoService();

    @POST
    @Path("login")
    public UsuarioResponse entrar(LoginRequest req) {
        return service.entrar(req);
    }
}
