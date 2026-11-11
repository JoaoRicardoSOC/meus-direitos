package br.com.fiap.meusdireitos.dto.response;

/**
 * Autor de uma denúncia ou comentário (o front usa o nome para as iniciais do avatar).
 */
public record AutorResponse(Long id, String nome) {
}
