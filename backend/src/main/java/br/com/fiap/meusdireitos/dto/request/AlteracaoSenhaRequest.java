package br.com.fiap.meusdireitos.dto.request;

/**
 * Troca de senha: exige a senha atual.
 */
public record AlteracaoSenhaRequest(String senhaAtual, String novaSenha) {
}
