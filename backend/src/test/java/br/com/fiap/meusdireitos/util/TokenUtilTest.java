package br.com.fiap.meusdireitos.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenUtilTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final Instant AGORA = Instant.parse("2026-11-12T12:00:00Z");

    @Test
    void tokenGeradoValidaEDevolveOId() {
        String token = TokenUtil.gerar(42L, SEGREDO, AGORA);
        assertEquals(42L, TokenUtil.validar(token, SEGREDO, AGORA));
        assertEquals(42L, TokenUtil.validar(token, SEGREDO, AGORA.plus(Duration.ofDays(7)).minusSeconds(1)));
    }

    @Test
    void formatoEhParte1PontoAssinaturaEmBase64Url() {
        String token = TokenUtil.gerar(42L, SEGREDO, AGORA);
        String[] partes = token.split("\\.");
        assertEquals(2, partes.length);
        String conteudo = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
        assertEquals("42." + AGORA.plus(Duration.ofDays(7)).getEpochSecond(), conteudo);
        assertTrue(partes[1].matches("[A-Za-z0-9_-]+"), "assinatura em base64url sem padding");
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String token = TokenUtil.gerar(42L, SEGREDO, AGORA);
        assertNull(TokenUtil.validar(token, SEGREDO, AGORA.plus(Duration.ofDays(7))));
        assertNull(TokenUtil.validar(token, SEGREDO, AGORA.plus(Duration.ofDays(30))));
    }

    @Test
    void outroSegredoNaoValida() {
        String token = TokenUtil.gerar(42L, SEGREDO, AGORA);
        assertNull(TokenUtil.validar(token, SEGREDO + "x", AGORA));
    }

    @Test
    void trocarOIdOuAValidadeInvalidaAAssinatura() {
        String token = TokenUtil.gerar(42L, SEGREDO, AGORA);
        String assinatura = token.substring(token.indexOf('.') + 1);
        long expira = AGORA.plus(Duration.ofDays(7)).getEpochSecond();

        String outroId = codificar("1." + expira) + "." + assinatura;
        String maisValidade = codificar("42." + (expira + 999_999)) + "." + assinatura;
        assertNull(TokenUtil.validar(outroId, SEGREDO, AGORA));
        assertNull(TokenUtil.validar(maisValidade, SEGREDO, AGORA));
    }

    @Test
    void formatosInvalidosDevolvemNull() {
        String valido = TokenUtil.gerar(42L, SEGREDO, AGORA);
        for (String invalido : new String[]{null, "", "   ", "42", "abc.def.ghi", ".", "abc.", ".abc",
                "***.***", valido + ".extra", valido.replace('.', '-'), "x".repeat(1000)}) {
            assertNull(TokenUtil.validar(invalido, SEGREDO, AGORA), String.valueOf(invalido));
        }
    }

    @Test
    void conteudoBemAssinadoMasMalFormadoEhRecusado() {
        // Simula alguém com o segredo gerando conteúdo inesperado: o validador não deve lançar exceção
        for (String conteudo : new String[]{"abc.123", "42", "42.abc", "-5.99999999999", "0.99999999999"}) {
            String parte1 = codificar(conteudo);
            String token = parte1 + "." + assinaturaDe(parte1);
            assertNull(TokenUtil.validar(token, SEGREDO, AGORA), conteudo);
        }
    }

    @Test
    void tokensDeUsuariosDiferentesSaoDiferentes() {
        assertNotEquals(TokenUtil.gerar(1L, SEGREDO, AGORA), TokenUtil.gerar(2L, SEGREDO, AGORA));
    }

    @Test
    void usaOSegredoDaConfiguracao() {
        // Sem token.segredo configurado nos testes, a API gera um segredo aleatório: gerar/validar continuam batendo
        String token = TokenUtil.gerar(7L);
        assertEquals(7L, TokenUtil.validar(token));
    }

    private static String codificar(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    /** Reaproveita o gerador para obter uma assinatura válida de um conteúdo arbitrário. */
    private static String assinaturaDe(String parte1) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(parte1.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
