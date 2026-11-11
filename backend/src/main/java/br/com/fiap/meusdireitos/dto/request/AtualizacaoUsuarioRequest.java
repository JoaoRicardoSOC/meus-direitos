package br.com.fiap.meusdireitos.dto.request;

/**
 * Dados editáveis da conta (tela "Editar meus dados"). A senha não é alterada por aqui.
 */
public record AtualizacaoUsuarioRequest(String nome, String email, String cidade, String uf) {
}
