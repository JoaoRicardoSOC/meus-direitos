package br.com.fiap.meusdireitos.util;

import br.com.fiap.meusdireitos.config.AppConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Token de sessão assinado, devolvido no login e no cadastro e enviado pelo front em
 * {@code Authorization: Bearer <token>}.
 * <p>
 * Formato (só JDK, sem biblioteca de JWT):
 * <pre>
 * base64url(idUsuario + "." + expiraEmEpochSegundos) + "." + base64url(HMAC-SHA256(segredo, parte1))
 * </pre>
 * A assinatura é calculada sobre a primeira parte já codificada. Sem conhecer o segredo
 * ({@link AppConfig#tokenSegredo()}), ninguém consegue trocar o id nem estender a validade.
 * O token não é criptografado: o id do usuário fica legível, o que não é segredo.
 */
public final class TokenUtil {

    /** Validade do token a partir do login/cadastro. */
    public static final Duration VALIDADE = Duration.ofDays(7);

    private static final String ALGORITMO = "HmacSHA256";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();
    /** Limite de tamanho para não processar valores absurdos recebidos no cabeçalho. */
    private static final int TAMANHO_MAXIMO = 256;

    private TokenUtil() {
    }

    /**
     * Gera um token válido por {@link #VALIDADE} para o usuário.
     *
     * @param idUsuario id do usuário que acabou de entrar ou se cadastrar
     * @return token no formato {@code parte1.assinatura}
     */
    public static String gerar(long idUsuario) {
        return gerar(idUsuario, AppConfig.tokenSegredo(), Instant.now());
    }

    /**
     * Confere formato, assinatura (em tempo constante) e validade.
     *
     * @param token valor recebido (sem o prefixo "Bearer ")
     * @return id do usuário, ou null se o token for inválido, adulterado ou estiver expirado
     */
    public static Long validar(String token) {
        return validar(token, AppConfig.tokenSegredo(), Instant.now());
    }

    /** Versão com segredo e instante explícitos (usada nos testes). */
    static String gerar(long idUsuario, String segredo, Instant agora) {
        long expiraEm = agora.plus(VALIDADE).getEpochSecond();
        String parte1 = CODIFICADOR.encodeToString(
                (idUsuario + "." + expiraEm).getBytes(StandardCharsets.UTF_8));
        return parte1 + "." + CODIFICADOR.encodeToString(assinar(segredo, parte1));
    }

    /** Versão com segredo e instante explícitos (usada nos testes). */
    static Long validar(String token, String segredo, Instant agora) {
        if (token == null || token.isBlank() || token.length() > TAMANHO_MAXIMO) {
            return null;
        }
        String[] partes = token.trim().split("\\.", -1);
        if (partes.length != 2 || partes[0].isEmpty() || partes[1].isEmpty()) {
            return null;
        }
        try {
            byte[] assinaturaRecebida = DECODIFICADOR.decode(partes[1]);
            // Confere a assinatura antes de interpretar o conteúdo, em tempo constante
            if (!MessageDigest.isEqual(assinar(segredo, partes[0]), assinaturaRecebida)) {
                return null;
            }
            String conteudo = new String(DECODIFICADOR.decode(partes[0]), StandardCharsets.UTF_8);
            String[] campos = conteudo.split("\\.", -1);
            if (campos.length != 2) {
                return null;
            }
            long idUsuario = Long.parseLong(campos[0]);
            long expiraEm = Long.parseLong(campos[1]);
            if (idUsuario <= 0 || agora.getEpochSecond() >= expiraEm) {
                return null;
            }
            return idUsuario;
        } catch (IllegalArgumentException e) {
            // Base64 inválido ou número inválido (NumberFormatException é subclasse)
            return null;
        }
    }

    private static byte[] assinar(String segredo, String parte1) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(parte1.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 faz parte de toda JVM; só falharia com um ambiente quebrado
            throw new IllegalStateException("HMAC-SHA256 indisponível na JVM", e);
        }
    }
}
