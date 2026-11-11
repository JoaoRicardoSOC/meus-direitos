package br.com.fiap.meusdireitos.dto.request;

/**
 * Confirmação da exclusão da conta: a pessoa digita a própria senha.
 */
public record ExclusaoContaRequest(String senha) {
}
