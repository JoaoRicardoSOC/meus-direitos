package br.com.fiap.meusdireitos.dto.request;

/**
 * Dados do cadastro conversacional (telas 02 a 05): nome, cidade/UF, e-mail e senha.
 */
public record CadastroUsuarioRequest(String nome, String email, String senha, String cidade, String uf) {
}
