package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.exception.NaoAutenticadoException;
import br.com.fiap.meusdireitos.util.TokenUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Só os caminhos que não consultam o banco (cabeçalho ausente ou token inválido). */
class AutenticacaoServiceTest {

    private final AutenticacaoService service = new AutenticacaoService();

    @Test
    void extraiOTokenDoCabecalhoBearer() {
        assertEquals("abc.def", AutenticacaoService.extrairBearer("Bearer abc.def"));
        assertEquals("abc.def", AutenticacaoService.extrairBearer("  bearer   abc.def  "));
        assertNull(AutenticacaoService.extrairBearer(null));
        assertNull(AutenticacaoService.extrairBearer(""));
        assertNull(AutenticacaoService.extrairBearer("Bearer "));
        assertNull(AutenticacaoService.extrairBearer("Basic dXNlcjpzZW5oYQ=="));
        assertNull(AutenticacaoService.extrairBearer("42")); // o antigo X-Usuario-Id não vale mais
    }

    @Test
    void idOpcionalDevolveOIdDeUmTokenValido() {
        assertEquals(9L, service.idOpcional("Bearer " + TokenUtil.gerar(9L)));
    }

    @Test
    void idOpcionalIgnoraCabecalhoAusenteOuInvalido() {
        assertNull(service.idOpcional(null));
        assertNull(service.idOpcional("42"));
        assertNull(service.idOpcional("Bearer token-falso"));
    }

    @Test
    void semCabecalhoPedeLogin() {
        NaoAutenticadoException e = assertThrows(NaoAutenticadoException.class,
                () -> service.idDoUsuarioLogado(null));
        assertEquals("Faça login para continuar.", e.getMessage());
        assertEquals(401, e.getStatusHttp());
        assertThrows(NaoAutenticadoException.class, () -> service.idDoUsuarioLogado("42"));
    }

    @Test
    void tokenInvalidoEncerraASessao() {
        NaoAutenticadoException e = assertThrows(NaoAutenticadoException.class,
                () -> service.idDoUsuarioLogado("Bearer abc.def"));
        assertEquals("Sua sessão não é mais válida. Entre novamente.", e.getMessage());
    }
}
