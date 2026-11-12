package br.com.fiap.meusdireitos.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextoUtilTest {

    @Test
    void grafiasDiferentesDaMesmaCidadeGeramAMesmaChave() {
        assertEquals("SAO PAULO", TextoUtil.chaveCidade("São Paulo"));
        assertEquals("SAO PAULO", TextoUtil.chaveCidade("  sao   paulo "));
        assertEquals("SAO PAULO", TextoUtil.chaveCidade("SÃO PAULO"));
        assertEquals("FLORIANOPOLIS", TextoUtil.chaveCidade("Florianópolis"));
        assertNull(TextoUtil.chaveCidade(null));
    }

    @Test
    void normalizaEspacosSemMexerEmAcentosNemMaiusculas() {
        assertEquals("São Paulo", TextoUtil.normalizarEspacos("  São   Paulo "));
        assertEquals("Rio de Janeiro", TextoUtil.normalizarEspacos("Rio\tde  Janeiro"));
        assertNull(TextoUtil.normalizarEspacos(null));
    }

    @Test
    void capitalizaNomesDeCidade() {
        assertEquals("São Paulo", TextoUtil.capitalizar("são paulo"));
        assertEquals("Rio de Janeiro", TextoUtil.capitalizar("RIO DE JANEIRO"));
        assertEquals("Vitória da Conquista", TextoUtil.capitalizar("  vitória   DA conquista "));
        assertNull(TextoUtil.capitalizar(null));
    }

    @Test
    void slugServeParaEmail() {
        assertEquals("sao.paulo", TextoUtil.slug("São Paulo"));
        assertEquals("santa.barbara.d.oeste", TextoUtil.slug("Santa Bárbara d'Oeste"));
        assertEquals("", TextoUtil.slug(null));
    }
}
