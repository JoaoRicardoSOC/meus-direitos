package br.com.fiap.meusdireitos.dto.request;

/**
 * Pedido das rotas de preparo da demonstração.
 *
 * @param eventos usado só no pulso forçado (1 a 10)
 */
public record SimulacaoRequest(String cidade, String uf, Integer eventos) {
}
