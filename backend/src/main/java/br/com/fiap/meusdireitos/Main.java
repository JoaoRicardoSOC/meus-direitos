package br.com.fiap.meusdireitos;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import br.com.fiap.meusdireitos.config.AppConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
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

    /** Hosts de loopback em que a API também escuta na outra família de endereços (IPv4/IPv6). */
    private static final Set<String> HOSTS_LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
    private static final int PORTA_PADRAO = 80;

    /**
     * Cria e inicia o servidor Grizzly com a configuração do Jersey.
     * O endereço vem de {@link AppConfig#serverUri()} (padrão: http://localhost:8080/api/).
     * Com host de loopback, também escuta no loopback da outra família, IPv4 ou IPv6
     * (ver {@link #escutarTambemNoOutroLoopback}).
     *
     * @return servidor HTTP em execução
     */
    public static HttpServer startServer() {
        final ResourceConfig config = new ResourceConfig().packages(PACOTE_BASE);
        URI uri = URI.create(AppConfig.serverUri());
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config);
        escutarTambemNoOutroLoopback(server, uri);
        return server;
    }

    /**
     * O Grizzly escuta só no endereço em que o host do {@code server.uri} resolve (para "localhost",
     * normalmente 127.0.0.1). No Windows, navegadores e o Node resolvem "localhost" primeiro para o IPv6
     * {@code ::1} e esperam ~2 s até desistir e tentar o IPv4. Para evitar essa espera, quando o host é de
     * loopback ({@code localhost}, {@code 127.0.0.1} ou {@code ::1}), a API também escuta, na mesma porta,
     * no loopback da outra família (IPv6 e IPv4). O {@code server.uri} continua valendo como antes.
     * <p>
     * Se o endereço extra não puder ser usado (máquina sem IPv6, porta ocupada), só registra um aviso:
     * a API segue no endereço principal.
     */
    static void escutarTambemNoOutroLoopback(HttpServer server, URI uri) {
        String host = semColchetes(uri.getHost());
        if (!HOSTS_LOOPBACK.contains(host.toLowerCase())) {
            return;
        }
        int porta = uri.getPort() == -1 ? PORTA_PADRAO : uri.getPort();
        InetAddress principal;
        try {
            principal = InetAddress.getByName(host); // o mesmo endereço que o Grizzly usou
        } catch (UnknownHostException e) {
            return;
        }
        for (String extra : List.of("127.0.0.1", "::1")) {
            try {
                InetAddress endereco = InetAddress.getByName(extra);
                if (endereco.equals(principal)) {
                    continue;
                }
                adicionarListener(server, endereco, porta);
            } catch (IOException | RuntimeException e) {
                LOG.warning(() -> "Não foi possível escutar também em " + extra + ", porta " + porta
                        + " (" + e.getMessage() + "). A API segue em " + uri + ".");
            }
        }
    }

    private static void adicionarListener(HttpServer server, InetAddress endereco, int porta) throws IOException {
        // Testa antes se o endereço e a porta estão livres: se o Grizzly falhar sozinho, ele só registra um SEVERE
        try (ServerSocket teste = new ServerSocket()) {
            teste.bind(new InetSocketAddress(endereco, porta));
        }
        NetworkListener listener = new NetworkListener("loopback-" + endereco.getHostAddress(),
                endereco.getHostAddress(), porta);
        server.addListener(listener); // com o servidor já rodando, o Grizzly configura e inicia o listener
        if (!listener.isStarted()) {
            server.removeListener(listener.getName());
            throw new IOException("o listener não iniciou");
        }
        LOG.info(() -> "API também escutando em " + endereco.getHostAddress() + ", porta " + porta + ".");
    }

    private static String semColchetes(String host) {
        if (host == null) {
            return "";
        }
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
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
