package br.com.fiap.meusdireitos.dto.response;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato com o front: o token só aparece no JSON do login e do cadastro. */
class UsuarioResponseTest {

    private final UsuarioResponse conta = new UsuarioResponse(1L, "Ana", "ana@exemplo.com", "Recife", "PE",
            LocalDateTime.of(2026, 11, 12, 10, 0), false, null, null);

    @Test
    void semTokenOCampoEhOmitido() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(conta);
            assertFalse(json.contains("\"token\""), json);
            assertFalse(json.contains("\"cidadePreparada\""), json);
        }
    }

    @Test
    void comTokenOCampoApareceEOsDemaisSeMantem() throws Exception {
        UsuarioResponse logada = conta.comToken("abc.def");
        assertEquals("abc.def", logada.token());
        assertEquals(conta.id(), logada.id());
        try (Jsonb jsonb = JsonbBuilder.create()) {
            assertTrue(jsonb.toJson(logada).contains("\"token\":\"abc.def\""));
        }
        assertEquals(Boolean.TRUE, logada.comCidadePreparada(true).cidadePreparada());
        assertEquals("abc.def", logada.comCidadePreparada(true).token());
    }
}
