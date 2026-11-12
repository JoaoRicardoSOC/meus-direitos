package br.com.fiap.meusdireitos.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * Cabeçalhos de segurança em toda resposta da API:
 * <ul>
 *     <li>{@code X-Content-Type-Options: nosniff}: o navegador respeita o Content-Type (JSON não vira HTML/script);</li>
 *     <li>{@code Referrer-Policy: no-referrer}: nenhum endereço é repassado a terceiros;</li>
 *     <li>{@code X-Frame-Options: DENY}: as respostas não podem ser exibidas dentro de um iframe;</li>
 *     <li>{@code Cache-Control: no-store}: dados pessoais não ficam guardados em cache do navegador ou de proxies.</li>
 * </ul>
 * O {@code Cache-Control} só é colocado quando a rota não definiu o seu: as listas fixas de
 * {@code MetadadosResource} respondem com {@code public, max-age=86400} e mantêm esse valor.
 * <p>
 * Assim como o {@link CorsFilter}, usa só a interface {@link ContainerResponseFilter} do JAX-RS.
 */
@Provider
public class CabecalhosSegurancaFilter implements ContainerResponseFilter {

    static final String SEM_CACHE = "no-store";

    @Override
    public void filter(ContainerRequestContext requisicao, ContainerResponseContext resposta) {
        MultivaluedMap<String, Object> cabecalhos = resposta.getHeaders();
        cabecalhos.putSingle("X-Content-Type-Options", "nosniff");
        cabecalhos.putSingle("Referrer-Policy", "no-referrer");
        cabecalhos.putSingle("X-Frame-Options", "DENY");
        if (!cabecalhos.containsKey(HttpHeaders.CACHE_CONTROL)) {
            cabecalhos.putSingle(HttpHeaders.CACHE_CONTROL, SEM_CACHE);
        }
    }
}
