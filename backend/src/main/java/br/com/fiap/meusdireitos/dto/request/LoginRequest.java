package br.com.fiap.meusdireitos.dto.request;

/**
 * Credenciais da tela "Entrar na sua conta".
 */
public record LoginRequest(String email, String senha) {
}
