package br.com.fiap.meusdireitos.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração e verificação de hash de senha com PBKDF2 (recurso nativo do JDK, sem bibliotecas extras).
 * <p>
 * A senha nunca é gravada: guardamos {@code iteracoes:salt:hash} (salt e hash em Base64).
 * O salt aleatório faz duas senhas iguais terem hashes diferentes, e as muitas iterações
 * tornam caro testar senhas por força bruta caso o banco vaze.
 */
public final class SenhaUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_SALT_BYTES = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaUtil() {
    }

    /**
     * @param senha senha em texto puro digitada pelo usuário
     * @return texto no formato "iteracoes:salt:hash", pronto para gravar no banco
     */
    public static String gerarHash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT_BYTES];
        ALEATORIO.nextBytes(salt);
        byte[] hash = calcular(senha, salt, ITERACOES);
        Base64.Encoder base64 = Base64.getEncoder();
        return ITERACOES + ":" + base64.encodeToString(salt) + ":" + base64.encodeToString(hash);
    }

    /**
     * @param senha        senha digitada no login
     * @param hashGravado  valor salvo no banco por {@link #gerarHash(String)}
     * @return true se a senha confere
     */
    public static boolean verificar(String senha, String hashGravado) {
        if (senha == null || hashGravado == null) {
            return false;
        }
        String[] partes = hashGravado.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[0]);
            Base64.Decoder base64 = Base64.getDecoder();
            byte[] salt = base64.decode(partes[1]);
            byte[] esperado = base64.decode(partes[2]);
            byte[] calculado = calcular(senha, salt, iteracoes);
            // comparação em tempo constante: não revela por tempo quantos bytes coincidem
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false; // hash gravado em formato inválido
        }
    }

    private static byte[] calcular(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de hash de senha indisponível no JDK", e);
        } finally {
            especificacao.clearPassword();
        }
    }
}
