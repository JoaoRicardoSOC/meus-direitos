package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.response.OpcaoResponse;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.service.MetadadosService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Listas de opções para os campos de seleção do front-end (categorias, raça/cor, etc.).
 * Mantém a tela sempre igual aos valores aceitos pela API e pelo banco.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MetadadosResource {

    private final MetadadosService service = new MetadadosService();

    @GET
    @Path("categorias")
    public List<OpcaoResponse> categorias() {
        return service.listar(CategoriaDenuncia.class);
    }

    @GET
    @Path("status-denuncia")
    public List<OpcaoResponse> statusDenuncia() {
        return service.listar(StatusDenuncia.class);
    }

    @GET
    @Path("racas-cor")
    public List<OpcaoResponse> racasCor() {
        return service.listar(RacaCor.class);
    }

    @GET
    @Path("deficiencias")
    public List<OpcaoResponse> deficiencias() {
        return service.listar(Deficiencia.class);
    }

    @GET
    @Path("percursos-escolares")
    public List<OpcaoResponse> percursosEscolares() {
        return service.listar(PercursoEscolar.class);
    }
}
