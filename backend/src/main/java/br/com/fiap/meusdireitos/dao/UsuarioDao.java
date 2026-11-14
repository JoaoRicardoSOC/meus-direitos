package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Usuario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Acesso aos dados de usuários (TB_GS26S2_USUARIO).
 */
public interface UsuarioDao extends CrudDao<Usuario> {

    /**
     * @param email e-mail já normalizado
     * @return o usuário, ou {@code null} se não houver conta com esse e-mail
     */
    Usuario buscarPorEmail(String email) throws SQLException;

    /** @return somente usuários reais (sem os moradores simulados), por nome */
    List<Usuario> listarReais() throws SQLException;

    /** @return todos os moradores simulados (para limpar a demonstração) */
    List<Usuario> listarSimulados() throws SQLException;

    /** @return moradores simulados de uma cidade */
    List<Usuario> listarSimuladosDaCidade(String cidade, String uf) throws SQLException;

    /** Grava vários usuários de uma vez (lote JDBC), para o conteúdo de demonstração. */
    void inserirEmLote(List<Usuario> usuarios) throws SQLException;

    /** @return mapa e-mail → id, dos e-mails informados que existirem */
    Map<String, Long> idsPorEmail(List<String> emails) throws SQLException;

    /** @return true se existe usuário com o id informado */
    boolean existe(Long id) throws SQLException;

    /** @return quantidade de denúncias registradas pelo usuário */
    int contarDenuncias(Long id) throws SQLException;
}
