package br.com.fiap.meusdireitos;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import br.com.fiap.meusdireitos.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ponto de entrada da API Meus Direitos.
 * <p>
 * Sobe um servidor HTTP Grizzly com o Jersey (implementação do JAX-RS),
 * expondo todos os recursos e providers encontrados no pacote base.
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    /** Pacote varrido pelo Jersey em busca de resources (@Path) e providers (@Provider). */
    private static final String PACOTE_BASE = "br.com.fiap.meusdireitos";

    /**
     * Cria e inicia o servidor Grizzly com a configuração do Jersey.
     * O endereço vem de {@link AppConfig#serverUri()} (padrão: http://localhost:8080/api/).
     *
     * @return servidor HTTP em execução
     */
    public static HttpServer startServer() {
        final ResourceConfig config = new ResourceConfig().packages(PACOTE_BASE);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(AppConfig.serverUri()), config);
    }

    public static void main(String[] args) throws IOException {
        List<String> pendencias = AppConfig.pendencias();
        if (!pendencias.isEmpty()) {
            // A API sobe mesmo assim (o front consegue trabalhar), mas o acesso ao banco vai falhar.
            LOG.warning("Configuração do banco incompleta. Defina " + pendencias
                    + " em backend/" + AppConfig.ARQUIVO_CONFIG + " (veja db.properties.example).");
        }

        final HttpServer server = startServer();

        // Garante que o servidor seja encerrado mesmo com Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        System.out.printf("API Meus Direitos rodando em %s%nPressione Enter (ou Ctrl+C) para encerrar...%n", AppConfig.serverUri());
        aguardarEncerramento();
        server.shutdownNow();
    }

    /**
     * Bloqueia até o usuário pressionar Enter.
     * Sem console (ex.: execução em segundo plano), a entrada termina na hora; nesse caso
     * a API continua rodando até ser interrompida com Ctrl+C (o shutdown hook encerra o servidor).
     */
    private static void aguardarEncerramento() throws IOException {
        if (System.in.read() != -1) {
            return;
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
