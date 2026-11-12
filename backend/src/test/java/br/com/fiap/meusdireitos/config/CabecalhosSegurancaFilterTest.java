package br.com.fiap.meusdireitos.config;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CabecalhosSegurancaFilterTest {

    /** Resposta mínima: só os cabeçalhos importam para o filtro (sem biblioteca de mocks). */
    private static ContainerResponseContext respostaCom(MultivaluedMap<String, Object> cabecalhos) {
        return (ContainerResponseContext) Proxy.newProxyInstance(
                CabecalhosSegurancaFilterTest.class.getClassLoader(),
                new Class<?>[]{ContainerResponseContext.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("getHeaders")) {
                        return cabecalhos;
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
    }

    @Test
    void colocaOsCabecalhosDeSegurancaENoStore() {
        MultivaluedMap<String, Object> cabecalhos = new MultivaluedHashMap<>();
        new CabecalhosSegurancaFilter().filter(null, respostaCom(cabecalhos));

        assertEquals("nosniff", cabecalhos.getFirst("X-Content-Type-Options"));
        assertEquals("no-referrer", cabecalhos.getFirst("Referrer-Policy"));
        assertEquals("DENY", cabecalhos.getFirst("X-Frame-Options"));
        assertEquals("no-store", cabecalhos.getFirst("Cache-Control"));
    }

    @Test
    void mantemOCacheControlDefinidoPelaRota() {
        MultivaluedMap<String, Object> cabecalhos = new MultivaluedHashMap<>();
        cabecalhos.putSingle("Cache-Control", "public, max-age=86400");
        new CabecalhosSegurancaFilter().filter(null, respostaCom(cabecalhos));

        assertEquals(1, cabecalhos.get("Cache-Control").size());
        assertEquals("public, max-age=86400", cabecalhos.getFirst("Cache-Control"));
        assertEquals("nosniff", cabecalhos.getFirst("X-Content-Type-Options"));
    }
}
