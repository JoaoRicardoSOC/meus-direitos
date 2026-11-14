package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.ReporteComentarioDao;
import br.com.fiap.meusdireitos.service.ComentarioService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * Implementação JDBC de {@link ReporteComentarioDao}.
 * <p>
 * O reporte repetido é recusado pela chave primária composta (ORA-00001); o DAO trata isso como
 * "já reportado", sem erro. Assim duas requisições simultâneas da mesma pessoa contam uma vez só.
 */
public class ReporteComentarioDaoJdbc implements ReporteComentarioDao {

    /**
     * Condição SQL "o comentário não está oculto" (menos de {@link ComentarioService#REPORTES_PARA_OCULTAR}
     * reportes). Usada na listagem de comentários e nas contagens de comentários das denúncias e relatórios.
     *
     * @param colunaComentario coluna com o código do comentário, ex.: "c.cd_comentario"
     */
    public static String naoOculto(String colunaComentario) {
        return "(SELECT COUNT(*) FROM tb_gs26s2_reporte_comentario rc WHERE rc.cd_comentario = " + colunaComentario
                + ") < " + ComentarioService.REPORTES_PARA_OCULTAR;
    }

    /** ORA-00001: chave primária ou única violada. */
    private static final int ORA_UNIQUE_VIOLADA = 1;

    private static final String SQL_INSERIR =
            "INSERT INTO tb_gs26s2_reporte_comentario (cd_usuario, cd_comentario, dt_reporte) VALUES (?, ?, SYSTIMESTAMP)";
    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM tb_gs26s2_reporte_comentario WHERE cd_comentario = ?";
    private static final String SQL_EXCLUIR_POR_COMENTARIO =
            "DELETE FROM tb_gs26s2_reporte_comentario WHERE cd_comentario = ?";
    private static final String SQL_EXCLUIR_POR_DENUNCIA =
            "DELETE FROM tb_gs26s2_reporte_comentario WHERE cd_comentario IN "
                    + "(SELECT cd_comentario FROM tb_gs26s2_comentario WHERE cd_denuncia = ?)";
    private static final String SQL_EXCLUIR_DO_USUARIO =
            "DELETE FROM tb_gs26s2_reporte_comentario WHERE cd_usuario = ? "
                    + "OR cd_comentario IN (SELECT cd_comentario FROM tb_gs26s2_comentario WHERE cd_usuario = ? "
                    + "OR cd_denuncia IN (SELECT cd_denuncia FROM tb_gs26s2_denuncia WHERE cd_usuario = ?))";

    private final Connection conexao;

    public ReporteComentarioDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public boolean reportar(Long idUsuario, Long idComentario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idComentario);
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == ORA_UNIQUE_VIOLADA) {
                return false; // a pessoa já tinha reportado este comentário
            }
            throw e; // ex.: ORA-02291, comentário ou usuário inexistente
        }
    }

    @Override
    public int contar(Long idComentario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_CONTAR)) {
            stmt.setLong(1, idComentario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void excluirPorComentario(Long idComentario) throws SQLException {
        executar(SQL_EXCLUIR_POR_COMENTARIO, idComentario, 1);
    }

    @Override
    public void excluirPorDenuncia(Long idDenuncia) throws SQLException {
        executar(SQL_EXCLUIR_POR_DENUNCIA, idDenuncia, 1);
    }

    @Override
    public void excluirDoUsuario(Long idUsuario) throws SQLException {
        executar(SQL_EXCLUIR_DO_USUARIO, idUsuario, 3);
    }

    /** Executa um DELETE cujo único valor se repete {@code vezes} vezes. */
    private void executar(String sql, Long id, int vezes) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            for (int i = 1; i <= vezes; i++) {
                stmt.setLong(i, id);
            }
            stmt.executeUpdate();
        }
    }
}
