package br.com.fiap.meusdireitos.dto.response;

/**
 * Autor de uma denúncia ou comentário (o front usa o nome para as iniciais do avatar).
 *
 * @param simulado true se é um morador fictício da demonstração "cidade viva" (o front mostra o selo "Exemplo")
 */
public record AutorResponse(Long id, String nome, boolean simulado) {
}
