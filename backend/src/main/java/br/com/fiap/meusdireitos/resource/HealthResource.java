package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.response.HealthResponse;
import br.com.fiap.meusdireitos.service.HealthService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Endpoint de saúde: permite ao front-end (e a quem avalia) confirmar que a API e o banco estão no ar.
 */
@Path("health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private final HealthService service = new HealthService();

    @GET
    public HealthResponse verificar() {
        return service.verificar();
    }
}
