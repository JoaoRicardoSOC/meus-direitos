package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Usuario;

import java.time.LocalDateTime;

/**
 * Dados da própria conta (nunca inclui a senha nem o hash).
 *
 * @param perfilPreenchido true se o usuário já respondeu o questionário de "Meus Direitos"
 * @param cidadePreparada  só no cadastro e na mudança de cidade: true se a cidade tem conteúdo de demonstração
 *                         ("cidade viva")
 * @param token            só no login e no cadastro: token de sessão assinado, a enviar em
 *                         {@code Authorization: Bearer <token>}. Nas demais respostas é null (campo omitido)
 */
public record UsuarioResponse(Long id, String nome, String email, String cidade, String uf,
                              LocalDateTime criadoEm, boolean perfilPreenchido, Boolean cidadePreparada,
                              String token) {

    public static UsuarioResponse de(Usuario usuario, boolean perfilPreenchido) {
        return de(usuario, perfilPreenchido, null);
    }

    public static UsuarioResponse de(Usuario usuario, boolean perfilPreenchido, Boolean cidadePreparada) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getCidade(),
                usuario.getUf(), usuario.getDataCadastro(), perfilPreenchido, cidadePreparada, null);
    }

    /** Cópia com {@code cidadePreparada} preenchido. */
    public UsuarioResponse comCidadePreparada(Boolean preparada) {
        return new UsuarioResponse(id, nome, email, cidade, uf, criadoEm, perfilPreenchido, preparada, token);
    }

    /** Cópia com o token de sessão (login e cadastro). */
    public UsuarioResponse comToken(String novoToken) {
        return new UsuarioResponse(id, nome, email, cidade, uf, criadoEm, perfilPreenchido, cidadePreparada,
                novoToken);
    }
}
