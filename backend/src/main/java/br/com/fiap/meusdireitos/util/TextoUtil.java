package br.com.fiap.meusdireitos.util;

import java.text.Normalizer;

/**
 * Normalização de textos para comparação.
 */
public final class TextoUtil {

    private TextoUtil() {
    }

    /**
     * Chave de comparação de nomes de cidade: maiúsculas, sem acentos e com espaços simples.
     * Ex.: "  são   paulo " e "Sao Paulo" → "SAO PAULO".
     *
     * @return a chave, ou null se o texto for nulo
     */
    public static String chaveCidade(String cidade) {
        if (cidade == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(cidade, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /**
     * Versão para e-mail: só letras e números minúsculos, separados por ponto.
     * Ex.: "São Paulo" → "sao.paulo".
     */
    public static String slug(String texto) {
        String chave = chaveCidade(texto);
        return chave == null ? "" : chave.toLowerCase().replaceAll("[^a-z0-9]+", ".").replaceAll("(^\\.|\\.$)", "");
    }
}
