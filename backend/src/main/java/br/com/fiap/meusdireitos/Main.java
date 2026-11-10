package br.com.fiap.meusdireitos;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Ponto de entrada da API Meus Direitos.
 * <p>
 * Sobe um servidor HTTP Grizzly com o Jersey (implementação do JAX-RS),
 * expondo todos os recursos e providers encontrados no pacote base.
 */
public class Main {

    /** Endereço base da API: todos os recursos ficam abaixo de /api. */
    public static final String BASE_URI = "http://localhost:8080/api/";

    /** Pacote varrido pelo Jersey em busca de resources (@Path) e providers (@Provider). */
    private static final String PACOTE_BASE = "br.com.fiap.meusdireitos";

    /**
     * Cria e inicia o servidor Grizzly com a configuração do Jersey.
     *
     * @return servidor HTTP em execução
     */
    public static HttpServer startServer() {
        final ResourceConfig config = new ResourceConfig().packages(PACOTE_BASE);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();

        // Garante que o servidor seja encerrado mesmo com Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        System.out.printf("API Meus Direitos rodando em %s%nPressione Enter para encerrar...%n", BASE_URI);
        System.in.read();
        server.shutdownNow();
    }
}
