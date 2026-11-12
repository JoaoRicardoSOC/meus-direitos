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
     * Tira os espaços das pontas e junta os espaços repetidos. Ex.: "  São   Paulo " → "São Paulo".
     * A cidade é gravada assim para que a comparação no banco ({@code JdbcUtil.cidadeIgual}) não precise
     * de expressão regular.
     *
     * @return o texto normalizado, ou null se o texto for nulo
     */
    public static String normalizarEspacos(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }

    /**
     * Nome próprio com iniciais maiúsculas, mantendo acentos e preposições em minúsculas.
     * Ex.: "são paulo" → "São Paulo"; "santa bárbara d'oeste" → "Santa Bárbara D'oeste".
     */
    public static String capitalizar(String texto) {
        if (texto == null) {
            return null;
        }
        StringBuilder resultado = new StringBuilder();
        for (String palavra : texto.trim().toLowerCase().split("\\s+")) {
            if (resultado.length() > 0) {
                resultado.append(' ');
            }
            boolean preposicao = palavra.matches("de|da|do|das|dos|e");
            resultado.append(preposicao || palavra.isEmpty() ? palavra
                    : Character.toUpperCase(palavra.charAt(0)) + palavra.substring(1));
        }
        return resultado.toString();
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
