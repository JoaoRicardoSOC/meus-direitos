package br.com.fiap.meusdireitos.model.relatorio;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatorioTest {

    @Test
    void percentualComUmaCasaDecimal() {
        assertEquals(33.3, Percentual.de(1, 3));
        assertEquals(66.7, Percentual.de(2, 3));
        assertEquals(100.0, Percentual.de(4, 4));
    }

    @Test
    void semDenunciasOPercentualEhZero() {
        assertEquals(0.0, Percentual.de(0, 0));
        assertEquals(0.0, new ResumoCidade(3, 0, 0, 0, 0, 0, 0, 0, 5, 0).percentualResolvidas());
    }

    @Test
    void indicadoresUsamOsTotaisDoRegistro() {
        assertEquals(25.0, new ResumoCidade(10, 8, 5, 1, 2, 30, 12, 6, 5, 4).percentualResolvidas());
        assertEquals(50.0, new TotalPorCategoria(CategoriaDenuncia.ASFALTO, 4, 1, 1, 2).percentualResolvidas());
        assertEquals(20.0, new TotalPorBairro("Centro", 5, 1, 12).percentualResolvidas());
    }

    @Test
    void engajamentoSomaVotosEComentarios() {
        assertEquals(17, new DenunciaEngajamento(1L, "Buraco", CategoriaDenuncia.ASFALTO,
                StatusDenuncia.ABERTA, 12, 10, 5).engajamento());
    }
}
