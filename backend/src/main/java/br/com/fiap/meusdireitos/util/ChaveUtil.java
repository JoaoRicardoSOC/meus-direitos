package br.com.fiap.meusdireitos.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Comparação das chaves das rotas restritas (administração do catálogo e preparo da demonstração).
 */
public final class ChaveUtil {

    private ChaveUtil() {
    }

    /**
     * Compara em tempo constante ({@link MessageDigest#isEqual}), para não revelar quantos caracteres
     * do início acertaram pelo tempo de resposta.
     *
     * @param configurada chave do db.properties; null ou vazia = rota desligada
     * @param informada   valor recebido no cabeçalho
     * @return true somente se houver chave configurada e as duas forem iguais
     */
    public static boolean confere(String configurada, String informada) {
        if (configurada == null || configurada.isBlank() || informada == null) {
            return false;
        }
        return MessageDigest.isEqual(configurada.getBytes(StandardCharsets.UTF_8),
                informada.getBytes(StandardCharsets.UTF_8));
    }
}
