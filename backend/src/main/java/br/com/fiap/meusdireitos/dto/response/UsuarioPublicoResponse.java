package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Usuario;

/**
 * Dados públicos de um usuário: sem e-mail e sem dados do perfil (que são sensíveis).
 *
 * @param totalDenuncias preenchido só na consulta individual (na listagem fica de fora do JSON)
 */
public record UsuarioPublicoResponse(Long id, String nome, String cidade, String uf, Integer totalDenuncias) {

    public static UsuarioPublicoResponse de(Usuario usuario, Integer totalDenuncias) {
        return new UsuarioPublicoResponse(usuario.getId(), usuario.getNome(), usuario.getCidade(),
                usuario.getUf(), totalDenuncias);
    }
}
