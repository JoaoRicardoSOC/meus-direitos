package br.com.fiap.meusdireitos.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilEnumsTest {

    @Test
    void ppiIncluiPretaPardaEIndigena() {
        assertTrue(RacaCor.PRETA.isPpi());
        assertTrue(RacaCor.PARDA.isPpi());
        assertTrue(RacaCor.INDIGENA.isPpi());
        assertFalse(RacaCor.BRANCA.isPpi());
        assertFalse(RacaCor.AMARELA.isPpi());
        assertFalse(RacaCor.NAO_INFORMADO.isPpi());
    }

    @Test
    void nenhumaENaoInformadoNaoContamComoDeficiencia() {
        assertFalse(Deficiencia.NENHUMA.isDeficiencia());
        assertFalse(Deficiencia.NAO_INFORMADO.isDeficiencia());
        assertTrue(Deficiencia.FISICA.isDeficiencia());
        assertTrue(Deficiencia.VISUAL.isDeficiencia());
    }
}
