package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventoLinhaDoTempoTest {

    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 11, 10, 9, 0);

    private static Denuncia denuncia(StatusDenuncia status, LocalDateTime atualizacao) {
        Denuncia d = new Denuncia();
        d.setStatus(status);
        d.setDataCriacao(CRIACAO);
        d.setDataAtualizacao(atualizacao);
        return d;
    }

    private static HistoricoStatus mudanca(StatusDenuncia de, StatusDenuncia para, OrigemStatus origem, LocalDateTime quando) {
        return new HistoricoStatus(null, 1L, de, para, origem, quando);
    }

    @Test
    void denunciaAbertaSemMudancasTemSoORegistro() {
        List<EventoLinhaDoTempo> passos = EventoLinhaDoTempo.montar(denuncia(StatusDenuncia.ABERTA, CRIACAO), List.of());
        assertEquals(1, passos.size());
        assertTrue(passos.get(0).registro());
        assertEquals(StatusDenuncia.ABERTA, passos.get(0).situacao());
        assertEquals(CRIACAO, passos.get(0).quando());
        assertNull(passos.get(0).origem());
    }

    @Test
    void mudancasEntramEmOrdemDepoisDoRegistro() {
        LocalDateTime dia12 = CRIACAO.plusDays(2);
        LocalDateTime dia14 = CRIACAO.plusDays(4);
        // fora de ordem de propósito: a linha do tempo ordena pela data
        List<EventoLinhaDoTempo> passos = EventoLinhaDoTempo.montar(denuncia(StatusDenuncia.RESOLVIDA, dia14), List.of(
                mudanca(StatusDenuncia.EM_ANDAMENTO, StatusDenuncia.RESOLVIDA, OrigemStatus.COMUNIDADE, dia14),
                mudanca(StatusDenuncia.ABERTA, StatusDenuncia.EM_ANDAMENTO, OrigemStatus.AUTOR, dia12)));

        assertEquals(3, passos.size());
        assertTrue(passos.get(0).registro());
        assertEquals(StatusDenuncia.EM_ANDAMENTO, passos.get(1).situacao());
        assertEquals(OrigemStatus.AUTOR, passos.get(1).origem());
        assertEquals(StatusDenuncia.RESOLVIDA, passos.get(2).situacao());
        assertEquals(OrigemStatus.COMUNIDADE, passos.get(2).origem());
        assertFalse(passos.get(2).registro());
    }

    @Test
    void reaberturaApareceComoNovaMudanca() {
        List<EventoLinhaDoTempo> passos = EventoLinhaDoTempo.montar(denuncia(StatusDenuncia.ABERTA, CRIACAO.plusDays(3)), List.of(
                mudanca(StatusDenuncia.ABERTA, StatusDenuncia.RESOLVIDA, OrigemStatus.AUTOR, CRIACAO.plusDays(1)),
                mudanca(StatusDenuncia.RESOLVIDA, StatusDenuncia.ABERTA, OrigemStatus.AUTOR, CRIACAO.plusDays(3))));
        assertEquals(3, passos.size());
        assertEquals(StatusDenuncia.ABERTA, passos.get(2).situacao());
        assertFalse(passos.get(2).registro());
    }

    @Test
    void denunciaAnteriorAoHistoricoMostraASituacaoAtualSemOrigem() {
        LocalDateTime atualizacao = CRIACAO.plusDays(5);
        List<EventoLinhaDoTempo> passos = EventoLinhaDoTempo.montar(denuncia(StatusDenuncia.RESOLVIDA, atualizacao), List.of());
        assertEquals(2, passos.size());
        assertEquals(StatusDenuncia.RESOLVIDA, passos.get(1).situacao());
        assertEquals(atualizacao, passos.get(1).quando());
        assertNull(passos.get(1).origem(), "sem histórico gravado, não se sabe quem mudou");
    }
}
