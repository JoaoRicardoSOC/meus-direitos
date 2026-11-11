package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Usuario;

import java.time.LocalDateTime;

/**
 * Dados da própria conta (nunca inclui a senha nem o hash).
 *
 * @param perfilPreenchido true se o usuário já respondeu o questionário de "Meus Direitos"
 * @param cidadePreparada  só no cadastro: true se a cidade tem conteúdo de demonstração ("cidade viva")
 */
public record UsuarioResponse(Long id, String nome, String email, String cidade, String uf,
                              LocalDateTime criadoEm, boolean perfilPreenchido, Boolean cidadePreparada) {

    public static UsuarioResponse de(Usuario usuario, boolean perfilPreenchido) {
        return de(usuario, perfilPreenchido, null);
    }

    public static UsuarioResponse de(Usuario usuario, boolean perfilPreenchido, Boolean cidadePreparada) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getCidade(),
                usuario.getUf(), usuario.getDataCadastro(), perfilPreenchido, cidadePreparada);
    }
}
