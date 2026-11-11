package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Usuario;

import java.sql.SQLException;

/**
 * Acesso aos dados de usuários (TB_USUARIO).
 */
public interface UsuarioDao extends CrudDao<Usuario> {

    /**
     * @param email e-mail já normalizado
     * @return o usuário, ou {@code null} se não houver conta com esse e-mail
     */
    Usuario buscarPorEmail(String email) throws SQLException;

    /** @return true se existe usuário com o id informado */
    boolean existe(Long id) throws SQLException;

    /** @return quantidade de denúncias registradas pelo usuário */
    int contarDenuncias(Long id) throws SQLException;
}
