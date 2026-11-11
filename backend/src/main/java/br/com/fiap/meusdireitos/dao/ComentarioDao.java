package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Comentario;

import java.sql.SQLException;
import java.util.List;

/**
 * Acesso aos comentários da "Conversa do bairro" (TB_COMENTARIO).
 */
public interface ComentarioDao extends CrudDao<Comentario> {

    /** @return comentários da denúncia, do mais antigo para o mais novo */
    List<Comentario> listarPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove todos os comentários de uma denúncia (usado ao excluí-la). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove os comentários escritos pelo usuário e os feitos nas denúncias dele (exclusão de conta). */
    void excluirDoUsuario(Long idUsuario) throws SQLException;
}
