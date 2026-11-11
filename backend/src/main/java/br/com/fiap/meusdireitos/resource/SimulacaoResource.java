package br.com.fiap.meusdireitos.resource;

import br.com.fiap.meusdireitos.config.AppConfig;
import br.com.fiap.meusdireitos.dto.request.SimulacaoRequest;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.SemeaduraCidade;
import br.com.fiap.meusdireitos.service.simulacao.SimulacaoService;
import br.com.fiap.meusdireitos.service.validacao.Validador;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Rotas para preparar e resetar a demonstração ("cidade viva"). Não são usadas pelo front-end.
 * <p>
 * Exigem o cabeçalho {@code X-Chave-Simulacao} igual a {@code simulacao.chave} do db.properties.
 * Sem chave configurada, ou com chave errada, respondem 404 (como se não existissem).
 */
@Path("simulacao")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulacaoResource {

    private static final String CABECALHO_CHAVE = "X-Chave-Simulacao";

    private final SimulacaoService service = new SimulacaoService();

    /** Semeia uma cidade agora (útil antes de gravar o pitch). */
    @POST
    @Path("cidades")
    public Map<String, Object> semear(@HeaderParam(CABECALHO_CHAVE) String chave, SimulacaoRequest req) {
        exigirChave(chave);
        validarCidade(req);
        boolean semeada = service.semear(req.cidade(), req.uf());
        return Map.of("cidade", req.cidade().trim(), "uf", req.uf().trim().toUpperCase(), "semeadaAgora", semeada,
                "mensagem", semeada ? "Cidade semeada com conteúdo de demonstração." : "Esta cidade já tinha sido semeada.");
    }

    @GET
    @Path("cidades")
    public List<SemeaduraCidade> cidades(@HeaderParam(CABECALHO_CHAVE) String chave) {
        exigirChave(chave);
        return service.listarCidades();
    }

    /** Gera atividade agora, sem esperar o intervalo de 10 minutos (para mostrar no vídeo). */
    @POST
    @Path("pulso")
    public Map<String, Object> pulso(@HeaderParam(CABECALHO_CHAVE) String chave, SimulacaoRequest req) {
        exigirChave(chave);
        validarCidade(req);
        int eventos = req.eventos() == null ? 3 : req.eventos();
        new Validador().intervalo("eventos", eventos, 1, 10).lancarSeHouverErros();
        return Map.of("eventosGerados", service.pulsar(req.cidade(), req.uf(), true, eventos));
    }

    /** Apaga todo o conteúdo simulado (dados reais não são afetados). */
    @DELETE
    public Map<String, Object> limpar(@HeaderParam(CABECALHO_CHAVE) String chave) {
        exigirChave(chave);
        return Map.of("moradoresSimuladosRemovidos", service.limparTudo());
    }

    /** INSERTs do pacote da cidade, para o script .sql de carga inicial. */
    @GET
    @Path("sql")
    @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String exportarSql(@HeaderParam(CABECALHO_CHAVE) String chave, @QueryParam("cidade") String cidade,
                              @QueryParam("uf") String uf) {
        exigirChave(chave);
        validarCidade(new SimulacaoRequest(cidade, uf, null));
        return service.exportarSql(cidade, uf);
    }

    private static void exigirChave(String informada) {
        String configurada = AppConfig.simulacaoChave();
        boolean confere = configurada != null && informada != null && MessageDigest.isEqual(
                configurada.getBytes(StandardCharsets.UTF_8), informada.getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            throw new EntidadeNaoEncontradaException("A rota solicitada não existe.");
        }
    }

    private static void validarCidade(SimulacaoRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador()
                .obrigatorio("cidade", req.cidade()).tamanho("cidade", req.cidade(), 2, 80)
                .obrigatorio("uf", req.uf()).uf("uf", req.uf())
                .lancarSeHouverErros();
    }
}
