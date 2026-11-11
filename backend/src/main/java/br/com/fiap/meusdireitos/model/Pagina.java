package br.com.fiap.meusdireitos.model;

import java.util.List;

/**
 * Um pedaço (página) de uma lista grande, com o total de itens para o front montar a paginação.
 *
 * @param <T> tipo dos itens
 */
public record Pagina<T>(int pagina, int tamanho, int total, List<T> itens) {

    /** @return true se existem itens depois desta página */
    public boolean temProxima() {
        return (long) pagina * tamanho < total;
    }
}
