package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.request.DireitoRequest;
import br.com.fiap.meusdireitos.dto.response.DireitoResponse;
import br.com.fiap.meusdireitos.service.DireitoService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
 * No MVP não há perfil de administrador: o cadastro é usado pela equipe e pelos dados iniciais.
 */
@Path("direitos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DireitoResource {

    private final DireitoService service = new DireitoService();

    @GET
    public List<DireitoResponse> listar(@QueryParam("tag") String tag) {
        return service.listar(tag);
    }

    @GET
    @Path("{id: \\d+}")
    public DireitoResponse buscar(@PathParam("id") Long id) {
        return service.buscar(id);
    }

    @POST
    public Response criar(DireitoRequest req, @Context UriInfo uriInfo) {
        DireitoResponse criado = service.criar(req);
        URI local = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(local).entity(criado).build();
    }

    @PUT
    @Path("{id: \\d+}")
    public DireitoResponse atualizar(@PathParam("id") Long id, DireitoRequest req) {
        return service.atualizar(id, req);
    }

    @DELETE
    @Path("{id: \\d+}")
    public Response excluir(@PathParam("id") Long id) {
        service.excluir(id);
        return Response.noContent().build();
    }
}
