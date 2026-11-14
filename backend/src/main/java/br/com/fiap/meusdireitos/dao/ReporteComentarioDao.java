package br.com.fiap.meusdireitos.dao;

import java.sql.SQLException;

/**
 * Acesso aos reportes de comentários ofensivos (TB_GS26S2_REPORTE_COMENTARIO, tabela associativa N:N entre
 * usuário e comentário). A chave é o par (usuário, comentário): cada pessoa reporta um comentário uma vez só.
 */
public interface ReporteComentarioDao {

    /**
     * Registra o reporte. Idempotente: reportar de novo o mesmo comentário não dá erro nem conta duas vezes.
     *
     * @return true se o reporte foi registrado agora; false se a pessoa já tinha reportado
     */
    boolean reportar(Long idUsuario, Long idComentario) throws SQLException;

    /** @return quantas pessoas reportaram o comentário */
    int contar(Long idComentario) throws SQLException;

    /** Remove os reportes de um comentário (usado ao excluí-lo). */
    void excluirPorComentario(Long idComentario) throws SQLException;

    /** Remove os reportes feitos nos comentários de uma denúncia (usado ao excluí-la). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /**
     * Remove os reportes feitos pelo usuário, os feitos nos comentários dele e os feitos em comentários
     * das denúncias dele (exclusão de conta).
     */
    void excluirDoUsuario(Long idUsuario) throws SQLException;
}
