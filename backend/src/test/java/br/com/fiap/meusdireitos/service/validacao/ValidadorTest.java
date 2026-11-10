package br.com.fiap.meusdireitos.service.validacao;

import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorTest {

    @Test
    void dadosValidosNaoLancamExcecao() {
        assertDoesNotThrow(() -> new Validador()
                .obrigatorio("nome", "Mariana Oliveira").tamanho("nome", "Mariana Oliveira", 3, 100)
                .email("email", "mariana@exemplo.com")
                .uf("uf", "sp")
                .dataNoPassado("dataNascimento", LocalDate.of(1994, 5, 10))
                .intervalo("tamanho", 10, 1, 50)
                .lancarSeHouverErros());
    }

    @Test
    void acumulaErrosDeVariosCamposDeUmaVez() {
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> new Validador()
                .obrigatorio("nome", "   ")
                .email("email", "mariana@")
                .uf("uf", "XX")
                .lancarSeHouverErros());

        List<ErroCampo> campos = e.getCampos();
        assertEquals(3, campos.size());
        assertEquals("nome", campos.get(0).campo());
        assertEquals("email", campos.get(1).campo());
        assertEquals("uf", campos.get(2).campo());
        assertEquals(400, e.getStatusHttp());
    }

    @Test
    void guardaApenasOPrimeiroErroDeCadaCampo() {
        Validador v = new Validador()
                .obrigatorio("titulo", null)
                .tamanho("titulo", null, 5, 120)
                .regra(false, "titulo", "segundo erro");

        assertEquals(1, v.getErros().size());
        assertEquals("Campo obrigatório.", v.getErros().get(0).mensagem());
    }

    @Test
    void tamanhoConsideraTextoSemEspacosNasPontas() {
        assertTrue(new Validador().tamanho("titulo", "  abc  ", 5, 120).temErros());
        assertFalse(new Validador().tamanho("titulo", "abcde", 5, 120).temErros());
        assertTrue(new Validador().tamanho("titulo", "x".repeat(121), 5, 120).temErros());
    }

    @Test
    void dataDeHojeOuFuturaNaoEhPassado() {
        assertTrue(new Validador().dataNoPassado("d", LocalDate.now()).temErros());
        assertTrue(new Validador().dataNoPassado("d", LocalDate.now().plusDays(1)).temErros());
    }

    @Test
    void validacoesOpcionaisIgnoramNulos() {
        assertFalse(new Validador()
                .tamanho("a", null, 1, 2).email("b", null).uf("c", null)
                .dataNoPassado("d", null).intervalo("e", null, 1, 2)
                .temErros());
    }

    @Test
    void corpoNuloLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> Validador.corpoObrigatorio(null));
        assertDoesNotThrow(() -> Validador.corpoObrigatorio(new Object()));
    }
}
