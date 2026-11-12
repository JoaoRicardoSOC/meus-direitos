package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.ComentarioRequest;
import br.com.fiap.meusdireitos.dto.response.ComentarioResponse;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.ComentarioService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Edição e exclusão de um comentário pelo próprio autor, e reporte de comentário ofensivo por outra pessoa.
 * (Listar e criar ficam em /denuncias/{id}/comentarios.)
 */
@Path("comentarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComentarioResource {

    private final ComentarioService service = new ComentarioService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    @PUT
    @Path("{id: \\d+}")
    public ComentarioResponse editar(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId,
                                     @PathParam("id") Long id, ComentarioRequest req) {
        return service.editar(autenticacao.idDoUsuarioLogado(usuarioId), id, req);
    }

    @DELETE
    @Path("{id: \\d+}")
    public Response excluir(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId,
                            @PathParam("id") Long id) {
        service.excluir(autenticacao.idDoUsuarioLogado(usuarioId), id);
        return Response.noContent().build();
    }

    /** Reporta o comentário como ofensivo: 204 (também se já tinha reportado). */
    @POST
    @Path("{id: \\d+}/reporte")
    public Response reportar(@HeaderParam(UsuarioResource.CABECALHO_USUARIO) String usuarioId,
                             @PathParam("id") Long id) {
        service.reportar(autenticacao.idDoUsuarioLogado(usuarioId), id);
        return Response.noContent().build();
    }
}
