package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.dto.response.RelatorioResponses;
import br.com.fiap.meusdireitos.service.AutenticacaoService;
import br.com.fiap.meusdireitos.service.RelatorioService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Relatórios da cidade. Públicos: a cidade vem de ?cidade=&uf= ou, se omitida, do usuário logado.
 */
@Path("relatorios")
@Produces(MediaType.APPLICATION_JSON)
public class RelatorioResource {

    private static final String USUARIO = UsuarioResource.CABECALHO_USUARIO;

    private final RelatorioService service = new RelatorioService();
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    @GET
    @Path("resumo")
    public RelatorioResponses.Resumo resumo(@HeaderParam(USUARIO) String usuarioId,
                                            @QueryParam("cidade") String cidade, @QueryParam("uf") String uf) {
        return service.resumo(cidade, uf, autenticacao.idOpcional(usuarioId));
    }

    @GET
    @Path("categorias")
    public List<RelatorioResponses.PorCategoria> porCategoria(@HeaderParam(USUARIO) String usuarioId,
                                                              @QueryParam("cidade") String cidade,
                                                              @QueryParam("uf") String uf) {
        return service.porCategoria(cidade, uf, autenticacao.idOpcional(usuarioId));
    }

    @GET
    @Path("bairros")
    public List<RelatorioResponses.PorBairro> porBairro(@HeaderParam(USUARIO) String usuarioId,
                                                        @QueryParam("cidade") String cidade,
                                                        @QueryParam("uf") String uf,
                                                        @QueryParam("minimo") String minimo) {
        return service.porBairro(cidade, uf, minimo, autenticacao.idOpcional(usuarioId));
    }

    @GET
    @Path("engajamento")
    public List<RelatorioResponses.Engajamento> engajamento(@HeaderParam(USUARIO) String usuarioId,
                                                            @QueryParam("cidade") String cidade,
                                                            @QueryParam("uf") String uf,
                                                            @QueryParam("limite") String limite) {
        return service.engajamento(cidade, uf, limite, autenticacao.idOpcional(usuarioId));
    }
}
