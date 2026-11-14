package br.com.fiap.meusdireitos.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void semPortaUsaOLocalhostDoDesenvolvimento() {
        assertEquals("http://localhost:8080/api/", AppConfig.uriPadrao(null));
        assertEquals("http://localhost:8080/api/", AppConfig.uriPadrao(" "));
        assertEquals("http://localhost:8080/api/", AppConfig.uriPadrao("abc"));
    }

    @Test
    void comPortaDoDeployEscutaEmTodasAsInterfaces() {
        assertEquals("http://0.0.0.0:10000/api/", AppConfig.uriPadrao("10000"));
        assertEquals("http://0.0.0.0:8080/api/", AppConfig.uriPadrao(" 8080 "));
    }
}
