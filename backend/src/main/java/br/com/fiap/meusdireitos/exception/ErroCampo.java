package br.com.fiap.meusdireitos.exception;

/**
 * Erro de validação associado a um campo do formulário.
 *
 * @param campo    nome do campo no JSON (ex.: "email")
 * @param mensagem mensagem clara para mostrar ao lado do campo
 */
public record ErroCampo(String campo, String mensagem) {
}
