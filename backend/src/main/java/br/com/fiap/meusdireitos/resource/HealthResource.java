package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.response.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;

/**
 * Endpoint de saúde: permite ao front-end (e a quem avalia) confirmar que a API está no ar.
 */
@Path("health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public HealthResponse verificar() {
        // A verificação real do banco entra quando a ConnectionFactory existir (Sprint 0, parte do Caio).
        return new HealthResponse("UP", "NAO_VERIFICADO", LocalDateTime.now());
    }
}
