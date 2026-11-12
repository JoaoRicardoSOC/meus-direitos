package br.com.fiap.meusdireitos;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Confere que, com host de loopback, a API atende em 127.0.0.1 e em ::1 na mesma porta.
 * Usa um recurso mínimo numa porta livre qualquer: não sobe a API de verdade nem acessa o banco.
 */
class MainLoopbackTest {

    @Path("ping")
    public static class Ping {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String ping() {
            return "ok";
        }
    }

    @Test
    void atendeEmIpv4EIpv6QuandoOHostEhLocalhost() throws IOException {
        assumeTrue(ipv6Disponivel(), "máquina sem IPv6 de loopback");
        int porta = portaLivre();
        URI uri = URI.create("http://localhost:" + porta + "/teste/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, new ResourceConfig(Ping.class));
        try {
            Main.escutarTambemNoOutroLoopback(server, uri);
            assertEquals("ok", get("http://127.0.0.1:" + porta + "/teste/ping"));
            assertEquals("ok", get("http://[::1]:" + porta + "/teste/ping"));
        } finally {
            server.shutdownNow();
        }
    }

    @Test
    void hostQueNaoEhLoopbackNaoGanhaListenerExtra() throws IOException {
        int porta = portaLivre();
        URI uri = URI.create("http://127.0.0.1:" + porta + "/teste/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, new ResourceConfig(Ping.class));
        try {
            int antes = server.getListeners().size();
            Main.escutarTambemNoOutroLoopback(server, URI.create("http://exemplo.com:" + porta + "/teste/"));
            assertEquals(antes, server.getListeners().size());
        } finally {
            server.shutdownNow();
        }
    }

    private static int portaLivre() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return s.getLocalPort();
        }
    }

    private static boolean ipv6Disponivel() {
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress(InetAddress.getByName("::1"), 0));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String get(String endereco) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(endereco).toURL().openConnection();
        con.setConnectTimeout(2000);
        con.setReadTimeout(5000);
        try (InputStream in = con.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            con.disconnect();
        }
    }
}
