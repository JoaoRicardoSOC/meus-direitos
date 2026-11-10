package br.com.fiap.meusdireitos.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Libera o acesso do front-end (React em outra porta) à API pelo navegador (CORS).
 * <p>
 * Sem esses cabeçalhos o navegador bloqueia as chamadas de http://localhost:5173 para
 * http://localhost:8080. Só as origens configuradas em {@code cors.origens} são liberadas,
 * em vez de liberar qualquer site com "*".
 * <p>
 * Implementado com a interface {@link ContainerResponseFilter} da própria especificação JAX-RS,
 * sem dependências adicionais.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private static final String METODOS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String CABECALHOS_PERMITIDOS = "Content-Type, X-Usuario-Id";
    private static final String CABECALHOS_EXPOSTOS = "Location";
    private static final String CACHE_PREFLIGHT_SEGUNDOS = "3600";

    private final List<String> origensPermitidas = AppConfig.corsOrigens();

    @Override
    public void filter(ContainerRequestContext requisicao, ContainerResponseContext resposta) {
        String origem = requisicao.getHeaderString("Origin");
        if (origem == null || !origensPermitidas.contains(origem)) {
            return; // requisição sem navegador (Insomnia/curl) ou origem não autorizada
        }

        MultivaluedMap<String, Object> cabecalhos = resposta.getHeaders();
        cabecalhos.putSingle("Access-Control-Allow-Origin", origem);
        cabecalhos.putSingle("Vary", "Origin");
        cabecalhos.putSingle("Access-Control-Allow-Methods", METODOS);
        cabecalhos.putSingle("Access-Control-Allow-Headers", CABECALHOS_PERMITIDOS);
        cabecalhos.putSingle("Access-Control-Expose-Headers", CABECALHOS_EXPOSTOS);
        cabecalhos.putSingle("Access-Control-Max-Age", CACHE_PREFLIGHT_SEGUNDOS);
    }
}
