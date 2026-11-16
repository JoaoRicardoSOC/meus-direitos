package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dto.response.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HealthServiceTest {

    @Test
    void apiVivaRespondeSemConsultarOBanco() {
        // roda sem banco configurado: se vivo() tocasse no pool, este teste falharia ou esperaria o tempo limite
        HealthResponse resposta = new HealthService().vivo();

        assertEquals(HealthService.UP, resposta.status());
        assertNull(resposta.banco(), "o estado do banco fica só em GET /api/health");
        assertNotNull(resposta.dataHora());
    }
}
