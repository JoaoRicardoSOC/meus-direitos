package br.com.fiap.meusdireitos.service.moderacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroTermosOfensivosTest {

    @Test
    void textoNormalNaoEhBarrado() {
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("Também notei esse buraco na rua."));
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("A prefeitura recolheu o lixo ontem."));
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo(null));
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("   "));
    }

    @Test
    void encontraTermoIgnorandoAcentoECaixa() {
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("Que IDIOTA fez isso"));
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("seu otário"));
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("DESGRAÇADO!"));
    }

    @Test
    void encontraTermoColadoEmPontuacao() {
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("porra, de novo?"));
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("foda-se a calçada"));
        assertTrue(FiltroTermosOfensivos.contemTermoOfensivo("(merda)"));
    }

    @Test
    void comparaPalavraInteiraParaNaoBarrarPalavrasInocentes() {
        // "computador" contém "puta"; "cuidado" e "acumulou" contêm "cu"; "Curitiba" começa com "cu"
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("O computador do posto quebrou"));
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("Cuidado: acumulou água em Curitiba"));
        assertFalse(FiltroTermosOfensivos.contemTermoOfensivo("Disputa na assembleia do bairro"));
    }

    @Test
    void listaJaEstaNormalizada() {
        for (String termo : FiltroTermosOfensivos.TERMOS) {
            assertEquals(FiltroTermosOfensivos.normalizar(termo), termo, "termo fora do padrão: " + termo);
            assertTrue(termo.matches("[a-z0-9]+"), "termo com separador: " + termo);
        }
    }
}
