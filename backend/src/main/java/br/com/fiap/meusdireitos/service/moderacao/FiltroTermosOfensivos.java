package br.com.fiap.meusdireitos.service.moderacao;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Filtro simples de palavrões e ofensas em comentários.
 * <p>
 * <b>Não é moderação completa.</b> Compara palavra por palavra com uma lista curta de termos comuns em
 * português, ignorando acentos e maiúsculas ("IDIOTA", "idiota" e "ídiota" são iguais). Não pega
 * grafias disfarçadas ("1d10ta"), ironia nem ofensas sem palavrão; para isso existe o reporte da
 * comunidade (comentários com 3 reportes ficam ocultos).
 * <p>
 * A comparação é por palavra inteira para não barrar palavras inocentes que contêm um termo
 * ("computador" contém "puta"). Palavras comuns em denúncias urbanas que também podem ser ofensa
 * ("lixo", "arrombada", "pica-pau") ficaram de fora de propósito.
 */
public final class FiltroTermosOfensivos {

    /** Termos já sem acento e em minúsculas, com plurais e variações frequentes. */
    static final Set<String> TERMOS = Set.of(
            "porra", "caralho", "krl", "merda", "merdas", "bosta", "bostas",
            "puta", "putas", "puto", "putos", "putaria", "pqp",
            "foda", "foder", "fodase", "fodido", "fodida", "vsf", "vtnc", "tnc",
            "cu", "cuzao", "buceta", "boceta", "piroca", "caceta", "cacete",
            "fdp", "filhodaputa",
            "idiota", "idiotas", "imbecil", "imbecis", "babaca", "babacas",
            "otario", "otaria", "otarios", "otarias", "cretino", "cretina", "cretinos", "cretinas",
            "escroto", "escrota", "escrotos", "escrotas", "vagabundo", "vagabunda", "vagabundos", "vagabundas",
            "desgracado", "desgracada", "desgracados", "desgracadas", "corno", "cornos",
            "retardado", "retardada", "retardados", "retardadas", "viado", "viados");

    private FiltroTermosOfensivos() {
    }

    /** @return true se o texto tiver algum termo da lista (texto nulo ou vazio não tem) */
    public static boolean contemTermoOfensivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        for (String palavra : normalizar(texto).split("[^a-z0-9]+")) {
            if (TERMOS.contains(palavra)) {
                return true;
            }
        }
        return false;
    }

    /** Minúsculas e sem acentos: "Você É" → "voce e". */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
