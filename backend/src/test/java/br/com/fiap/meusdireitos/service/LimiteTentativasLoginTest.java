package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.exception.MuitasTentativasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteTentativasLoginTest {

    private static final String EMAIL = "ana@exemplo.com";

    /** Relógio que os testes avançam à mão. */
    private static final class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-11-12T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final LimiteTentativasLogin limite = new LimiteTentativasLogin(relogio);

    private void falhar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            limite.verificar(EMAIL);
            limite.registrarFalha(EMAIL);
        }
    }

    @Test
    void quatroFalhasAindaNaoBloqueiam() {
        falhar(4);
        assertDoesNotThrow(() -> limite.verificar(EMAIL));
    }

    @Test
    void cincoFalhasBloqueiamPorQuinzeMinutos() {
        falhar(5);
        MuitasTentativasException e = assertThrows(MuitasTentativasException.class, () -> limite.verificar(EMAIL));
        assertEquals(429, e.getStatusHttp());
        assertEquals("Muitas tentativas", e.getTitulo());
        assertEquals("Muitas tentativas de login. Tente de novo em 15 minutos.", e.getMessage());

        relogio.avancar(Duration.ofMinutes(14).plusSeconds(1));
        e = assertThrows(MuitasTentativasException.class, () -> limite.verificar(EMAIL));
        assertEquals("Muitas tentativas de login. Tente de novo em 1 minuto.", e.getMessage());

        relogio.avancar(Duration.ofSeconds(59));
        assertDoesNotThrow(() -> limite.verificar(EMAIL));
    }

    @Test
    void minutosRestantesSaoArredondadosParaCima() {
        falhar(5);
        relogio.avancar(Duration.ofMinutes(5).plusSeconds(10)); // faltam 9 min 50 s
        MuitasTentativasException e = assertThrows(MuitasTentativasException.class, () -> limite.verificar(EMAIL));
        assertEquals("Muitas tentativas de login. Tente de novo em 10 minutos.", e.getMessage());
    }

    @Test
    void depoisDoBloqueioOContadorRecomeca() {
        falhar(5);
        relogio.avancar(Duration.ofMinutes(15));
        falhar(4);
        assertDoesNotThrow(() -> limite.verificar(EMAIL));
        falhar(1);
        assertThrows(MuitasTentativasException.class, () -> limite.verificar(EMAIL));
    }

    @Test
    void sucessoZeraAsFalhas() {
        falhar(4);
        limite.registrarSucesso(EMAIL);
        falhar(4);
        assertDoesNotThrow(() -> limite.verificar(EMAIL));
    }

    @Test
    void falhaDuranteOBloqueioNaoEncurtaNemDesfazOBloqueio() {
        falhar(5);
        limite.registrarFalha(EMAIL); // requisição simultânea que passou da verificação
        assertThrows(MuitasTentativasException.class, () -> limite.verificar(EMAIL));
    }

    @Test
    void falhasAntigasDeixamDeContar() {
        falhar(4);
        relogio.avancar(Duration.ofMinutes(16));
        falhar(4);
        assertDoesNotThrow(() -> limite.verificar(EMAIL));
    }

    @Test
    void cadaEmailTemSeuContador() {
        falhar(5);
        assertDoesNotThrow(() -> limite.verificar("outra@exemplo.com"));
    }

    @Test
    void entradasVencidasSaoRemovidasDoMapa() {
        for (int i = 0; i < 100; i++) {
            limite.registrarFalha("pessoa" + i + "@exemplo.com");
        }
        assertEquals(100, limite.tamanho());
        relogio.avancar(Duration.ofMinutes(16));
        limite.registrarFalha(EMAIL); // dispara a limpeza
        assertEquals(1, limite.tamanho());
    }
}
