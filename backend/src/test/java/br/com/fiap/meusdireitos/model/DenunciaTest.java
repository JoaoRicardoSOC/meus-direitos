package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DenunciaTest {

    @Test
    void protocoloUsaAnoDaCriacaoEIdComQuatroDigitos() {
        Denuncia d = new Denuncia();
        d.setId(142L);
        d.setDataCriacao(LocalDateTime.of(2026, 11, 12, 14, 3));
        assertEquals("2026-0142", d.getProtocolo());

        d.setId(12345L);
        assertEquals("2026-12345", d.getProtocolo());
    }

    @Test
    void denunciaNaoGravadaNaoTemProtocolo() {
        assertNull(new Denuncia().getProtocolo());
    }

    @Test
    void somenteOAutorPodeAlterar() {
        Denuncia d = new Denuncia();
        d.setIdAutor(7L);
        assertTrue(d.isAutor(7L));
        assertFalse(d.isAutor(8L));
        assertFalse(d.isAutor(null));
    }

    @Test
    void setterNormalizaTextos() {
        Denuncia d = new Denuncia();
        d.setUf(" sp ");
        d.setBairro("   ");
        d.setTitulo("  Buraco na via  ");
        assertEquals("SP", d.getUf());
        assertNull(d.getBairro());
        assertEquals("Buraco na via", d.getTitulo());
    }

    @Test
    void denunciaResolvidaNaoAceitaNovasConfirmacoes() {
        Denuncia d = new Denuncia();
        assertTrue(d.aceitaConfirmacaoDeResolucao()); // ABERTA por padrão
        d.setStatus(StatusDenuncia.EM_ANDAMENTO);
        assertTrue(d.aceitaConfirmacaoDeResolucao());
        d.setStatus(StatusDenuncia.RESOLVIDA);
        assertFalse(d.aceitaConfirmacaoDeResolucao());
    }

    @Test
    void tresConfirmacoesResolvem() {
        assertFalse(Denuncia.confirmacoesSuficientes(2));
        assertTrue(Denuncia.confirmacoesSuficientes(3));
    }

    @Test
    void comparaCidadeSemDiferenciarMaiusculas() {
        Denuncia d = new Denuncia();
        d.setCidade("São Paulo");
        d.setUf("SP");
        assertTrue(d.ehDaCidade(" são paulo ", "sp"));
        assertTrue(d.ehDaCidade("Sao  Paulo", "SP")); // sem acento e com espaço repetido
        assertFalse(d.ehDaCidade("Campinas", "SP"));
        assertFalse(d.ehDaCidade("São Paulo", "RJ"));
        assertFalse(d.ehDaCidade(null, "SP"));
    }

    @Test
    void saldoDoPlacarDescontaDiscordancias() {
        assertEquals(116, new Placar(128, 12, TipoVoto.UP).saldo());
        assertEquals(-3, new Placar(0, 3, null).saldo());
    }

    @Test
    void paginaSabeSeHaProxima() {
        assertTrue(new Pagina<>(1, 10, 37, List.of()).temProxima());
        assertFalse(new Pagina<>(4, 10, 37, List.of()).temProxima());
        assertFalse(new Pagina<>(1, 10, 0, List.of()).temProxima());
    }
}
