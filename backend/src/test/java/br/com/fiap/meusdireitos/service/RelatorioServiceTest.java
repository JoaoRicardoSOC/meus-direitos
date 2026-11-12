package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Escolha da cidade dos relatórios nos caminhos que não consultam o banco. */
class RelatorioServiceTest {

    private final RelatorioService service = new RelatorioService();

    @Test
    void cidadeEUfInformadasSaoUsadasComoVieram() {
        RelatorioService.Cidade cidade = service.resolverCidade("  Campinas ", "sp", 7L);
        assertEquals("Campinas", cidade.nome());
        assertEquals("SP", cidade.uf());
    }

    @Test
    void soACidadeApontaAUfQueFaltou() {
        assertEquals(List.of("uf"), camposComErro(() -> service.resolverCidade("Campinas", null, 7L)));
    }

    @Test
    void soAUfApontaACidadeQueFaltou() {
        assertEquals(List.of("cidade"), camposComErro(() -> service.resolverCidade(" ", "SP", null)));
    }

    @Test
    void semCidadeESemLoginPedeOsDois() {
        assertEquals(List.of("cidade", "uf"), camposComErro(() -> service.resolverCidade(null, null, null)));
    }

    private static List<String> camposComErro(Runnable chamada) {
        ValidacaoException e = assertThrows(ValidacaoException.class, chamada::run);
        return e.getCampos().stream().map(ErroCampo::campo).toList();
    }
}
