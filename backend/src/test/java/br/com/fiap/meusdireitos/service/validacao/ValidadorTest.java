package br.com.fiap.meusdireitos.service.validacao;

import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void converteEnumSemDiferenciarMaiusculas() {
        assertEquals(TagDemografica.PPI, Validador.converterEnum(TagDemografica.class, " ppi ", "tag"));
        assertNull(Validador.converterEnum(TagDemografica.class, "", "tag"));
        assertNull(Validador.converterEnum(TagDemografica.class, null, "tag"));
    }

    @Test
    void enumInexistenteInformaValoresAceitos() {
        ValidacaoException e = assertThrows(ValidacaoException.class,
                () -> Validador.converterEnum(TagDemografica.class, "xyz", "tag"));
        assertEquals("tag", e.getCampos().get(0).campo());
        assertTrue(e.getCampos().get(0).mensagem().contains("IDOSO"));
    }

    @Test
    void urlPrecisaComecarComHttp() {
        assertFalse(new Validador().url("link", "https://www.gov.br").temErros());
        assertFalse(new Validador().url("link", "").temErros());
        assertTrue(new Validador().url("link", "ftp://x").temErros());
        assertTrue(new Validador().url("link", "https://com espaco").temErros());
    }

    @Test
    void corpoNuloLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> Validador.corpoObrigatorio(null));
        assertDoesNotThrow(() -> Validador.corpoObrigatorio(new Object()));
    }

    @Test
    void imagemAceitaSomenteFotosDeDemonstracao() {
        assertDoesNotThrow(() -> new Validador()
                .imagem("imagemUrl", "img/demo/asfalto-1.jpg")
                .imagem("imagemUrl", "img/demo/calcada-acessibilidade-2.jpeg")
                .imagem("imagemUrl", "img/demo/praca_3.png")
                .imagem("imagemUrl", " img/demo/onibus.webp ")
                .imagem("imagemUrl", null)
                .imagem("imagemUrl", "  ")
                .lancarSeHouverErros());
    }

    @Test
    void imagemRecusaEnderecosExternosEsquemasPerigososECaminhosForaDaPasta() {
        for (String invalida : List.of("https://exemplo.com/foto.png", "http://exemplo.com/img/demo/a.jpg",
                "javascript:alert(1)", "data:image/png;base64,AAA", "../../etc/passwd",
                "//outro-site.com/x.png", "img/demo/a b.jpg", "img/demo/../segredo.jpg", "img/demo/..jpg",
                "img/demo/sub/foto.jpg", "img/demo//foto.jpg", "/img/demo/foto.jpg", "img/outra/foto.jpg",
                "img/demo/foto.gif", "img/demo/foto.svg", "img/demo/.jpg", "img/demo/foto.jpg?x=1",
                "img/demo/" + "a".repeat(300) + ".jpg")) {
            ValidacaoException e = assertThrows(ValidacaoException.class,
                    () -> new Validador().imagem("imagemUrl", invalida).lancarSeHouverErros(), invalida);
            assertEquals("imagemUrl", e.getCampos().get(0).campo());
            assertEquals("Escolha uma das fotos de demonstração.", e.getCampos().get(0).mensagem());
        }
    }
}
