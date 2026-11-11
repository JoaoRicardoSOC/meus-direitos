package br.com.fiap.meusdireitos.dto.response;

import java.util.List;

/**
 * Envelope das listagens paginadas.
 *
 * @param total      quantidade total de itens (todas as páginas)
 * @param temProxima true se existe uma próxima página (o front mostra "carregar mais")
 * @param <T>        tipo dos itens
 */
public record PaginaResponse<T>(int pagina, int tamanho, int total, boolean temProxima, List<T> itens) {
}
