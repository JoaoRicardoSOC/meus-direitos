package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.response.OpcaoResponse;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.EnumDescritivo;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.service.MetadadosService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Listas de opções para os campos de seleção do front-end (categorias, raça/cor, etc.).
 * Mantém a tela sempre igual aos valores aceitos pela API e pelo banco.
 * <p>
 * As listas só mudam com uma nova versão da API, por isso podem ficar em cache por um dia
 * ({@value #CACHE_LISTAS_FIXAS}). As demais rotas respondem com {@code no-store}
 * (ver {@code CabecalhosSegurancaFilter}).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MetadadosResource {

    /** Cache-Control das listas fixas: qualquer cache pode guardar por 24 horas. */
    static final String CACHE_LISTAS_FIXAS = "public, max-age=86400";

    private final MetadadosService service = new MetadadosService();

    @GET
    @Path("categorias")
    public Response categorias() {
        return listaFixa(CategoriaDenuncia.class);
    }

    @GET
    @Path("status-denuncia")
    public Response statusDenuncia() {
        return listaFixa(StatusDenuncia.class);
    }

    @GET
    @Path("racas-cor")
    public Response racasCor() {
        return listaFixa(RacaCor.class);
    }

    @GET
    @Path("deficiencias")
    public Response deficiencias() {
        return listaFixa(Deficiencia.class);
    }

    @GET
    @Path("percursos-escolares")
    public Response percursosEscolares() {
        return listaFixa(PercursoEscolar.class);
    }

    private <E extends Enum<E> & EnumDescritivo> Response listaFixa(Class<E> tipo) {
        return Response.ok(new GenericEntity<List<OpcaoResponse>>(service.listar(tipo)) { })
                .header(HttpHeaders.CACHE_CONTROL, CACHE_LISTAS_FIXAS)
                .build();
    }
}
