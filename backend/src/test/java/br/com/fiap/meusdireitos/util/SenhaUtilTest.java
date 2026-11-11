package br.com.fiap.meusdireitos.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaUtilTest {

    @Test
    void senhaCorretaConfere() {
        String hash = SenhaUtil.gerarHash("senha1234");
        assertTrue(SenhaUtil.verificar("senha1234", hash));
    }

    @Test
    void senhaErradaNaoConfere() {
        String hash = SenhaUtil.gerarHash("senha1234");
        assertFalse(SenhaUtil.verificar("senha1235", hash));
        assertFalse(SenhaUtil.verificar("", hash));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentes() {
        assertNotEquals(SenhaUtil.gerarHash("senha1234"), SenhaUtil.gerarHash("senha1234"));
    }

    @Test
    void hashNaoContemASenhaEmTextoPuro() {
        String hash = SenhaUtil.gerarHash("senha1234");
        assertFalse(hash.contains("senha1234"));
        assertEquals(3, hash.split(":").length);
    }

    @Test
    void valoresInvalidosNaoConferemNemLancamExcecao() {
        assertFalse(SenhaUtil.verificar(null, SenhaUtil.gerarHash("x")));
        assertFalse(SenhaUtil.verificar("x", null));
        assertFalse(SenhaUtil.verificar("x", "formato-invalido"));
        assertFalse(SenhaUtil.verificar("x", "abc:###:###"));
    }
}
