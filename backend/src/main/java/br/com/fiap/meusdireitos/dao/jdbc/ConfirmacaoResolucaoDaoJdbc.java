package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.ConfirmacaoResolucaoDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementação JDBC de {@link ConfirmacaoResolucaoDao}.
 * A chave primária (usuário, denúncia) garante uma confirmação por pessoa: repetir não duplica.
 */
public class ConfirmacaoResolucaoDaoJdbc implements ConfirmacaoResolucaoDao {

    private static final String SQL_INSERIR =
            "INSERT INTO tb_confirmacao_resolucao (cd_usuario, cd_denuncia, dt_confirmacao) VALUES (?, ?, ?)";
    private static final String SQL_RETIRAR =
            "DELETE FROM tb_confirmacao_resolucao WHERE cd_usuario = ? AND cd_denuncia = ?";
    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM tb_confirmacao_resolucao WHERE cd_denuncia = ?";
    private static final String SQL_EXCLUIR_POR_DENUNCIA =
            "DELETE FROM tb_confirmacao_resolucao WHERE cd_denuncia = ?";

    private final Connection conexao;

    public ConfirmacaoResolucaoDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public boolean confirmar(Long idUsuario, Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idDenuncia);
            JdbcUtil.setDataHora(stmt, 3, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException jaConfirmada) {
            // ORA-00001 na chave primária: a pessoa já tinha confirmado (operação idempotente).
            // Uma FK inválida (ORA-02291) também cai aqui, mas o service valida a denúncia antes.
            return false;
        }
    }

    @Override
    public boolean retirar(Long idUsuario, Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_RETIRAR)) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idDenuncia);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public int contar(Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_CONTAR)) {
            stmt.setLong(1, idDenuncia);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void excluirPorDenuncia(Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_POR_DENUNCIA)) {
            stmt.setLong(1, idDenuncia);
            stmt.executeUpdate();
        }
    }
}
