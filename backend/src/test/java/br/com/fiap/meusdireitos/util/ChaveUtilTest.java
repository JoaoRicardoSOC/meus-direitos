package br.com.fiap.meusdireitos.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaveUtilTest {

    @Test
    void confereSomenteQuandoAsChavesSaoIguais() {
        assertTrue(ChaveUtil.confere("chave-secreta", "chave-secreta"));
        assertFalse(ChaveUtil.confere("chave-secreta", "chave-errada"));
        assertFalse(ChaveUtil.confere("chave-secreta", "CHAVE-SECRETA"));
        assertFalse(ChaveUtil.confere("chave-secreta", null));
    }

    @Test
    void semChaveConfiguradaNadaConfere() {
        assertFalse(ChaveUtil.confere(null, "qualquer"));
        assertFalse(ChaveUtil.confere("", ""));
        assertFalse(ChaveUtil.confere("   ", "   "));
    }
}
