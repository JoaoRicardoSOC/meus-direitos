package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Usuario;

import java.time.LocalDateTime;

/**
 * Dados da própria conta (nunca inclui a senha nem o hash).
 *
 * @param perfilPreenchido true se o usuário já respondeu o questionário de "Meus Direitos"
 */
public record UsuarioResponse(Long id, String nome, String email, String cidade, String uf,
                              LocalDateTime criadoEm, boolean perfilPreenchido) {

    public static UsuarioResponse de(Usuario usuario, boolean perfilPreenchido) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getCidade(),
                usuario.getUf(), usuario.getDataCadastro(), perfilPreenchido);
    }
}
