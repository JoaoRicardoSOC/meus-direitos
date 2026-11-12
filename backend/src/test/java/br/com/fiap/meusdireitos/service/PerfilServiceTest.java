package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dto.request.PerfilRequest;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import br.com.fiap.meusdireitos.model.enums.RacaCor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regras de validação do questionário (sem banco). */
class PerfilServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 12);
    private static final LocalDate NASCIMENTO = LocalDate.of(2000, 1, 1);

    @Test
    void dadoSensivelSemConsentimentoEhRecusado() {
        PerfilRequest[] semConsentimento = {
                new PerfilRequest(NASCIMENTO, null, null, null, null, null),
                new PerfilRequest(null, RacaCor.values()[0], null, null, null, false),
                new PerfilRequest(null, null, null, null, false, null),
        };
        for (PerfilRequest req : semConsentimento) {
            ValidacaoException e = assertThrows(ValidacaoException.class, () -> PerfilService.validar(req, HOJE));
            assertEquals("consentimento", e.getCampos().get(0).campo());
            assertEquals(PerfilService.MSG_SEM_CONSENTIMENTO, e.getCampos().get(0).mensagem());
        }
    }

    @Test
    void comConsentimentoOuSemDadosSensiveisPassa() {
        assertDoesNotThrow(() -> PerfilService.validar(
                new PerfilRequest(NASCIMENTO, RacaCor.values()[0], null, null, true, true), HOJE));
        assertDoesNotThrow(() -> PerfilService.validar(
                new PerfilRequest(null, null, null, null, null, null), HOJE));
    }

    @Test
    void dataDeNascimentoContinuaValidada() {
        ValidacaoException e = assertThrows(ValidacaoException.class, () -> PerfilService.validar(
                new PerfilRequest(HOJE.minusYears(130), null, null, null, null, true), HOJE));
        assertEquals("dataNascimento", e.getCampos().get(0).campo());
    }
}
