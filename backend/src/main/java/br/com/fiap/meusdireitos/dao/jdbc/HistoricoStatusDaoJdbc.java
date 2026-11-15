package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.HistoricoStatusDao;
import br.com.fiap.meusdireitos.model.HistoricoStatus;
import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de {@link HistoricoStatusDao}.
 */
public class HistoricoStatusDaoJdbc implements HistoricoStatusDao {

    private static final String SQL_INSERIR =
            "INSERT INTO tb_gs26s2_historico_status (cd_historico, cd_denuncia, st_anterior, st_novo, tp_origem, "
                    + "dt_alteracao) VALUES (seq_gs26s2_historico_status.NEXTVAL, ?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR_POR_DENUNCIA =
            "SELECT cd_historico, cd_denuncia, st_anterior, st_novo, tp_origem, dt_alteracao "
                    + "FROM tb_gs26s2_historico_status WHERE cd_denuncia = ? ORDER BY dt_alteracao, cd_historico";
    private static final String SQL_EXCLUIR_POR_DENUNCIA =
            "DELETE FROM tb_gs26s2_historico_status WHERE cd_denuncia = ?";
    private static final String SQL_EXCLUIR_DO_AUTOR =
            "DELETE FROM tb_gs26s2_historico_status "
                    + "WHERE cd_denuncia IN (SELECT cd_denuncia FROM tb_gs26s2_denuncia WHERE cd_usuario = ?)";

    private final Connection conexao;

    public HistoricoStatusDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public void registrar(Long idDenuncia, StatusDenuncia anterior, StatusDenuncia nova, OrigemStatus origem)
            throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            preencher(stmt, new HistoricoStatus(null, idDenuncia, anterior, nova, origem,
                    LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
            stmt.executeUpdate();
        }
    }

    @Override
    public void registrarEmLote(List<HistoricoStatus> mudancas) throws SQLException {
        if (mudancas.isEmpty()) {
            return;
        }
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            for (HistoricoStatus mudanca : mudancas) {
                preencher(stmt, mudanca);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public List<HistoricoStatus> listarPorDenuncia(Long idDenuncia) throws SQLException {
        List<HistoricoStatus> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR_POR_DENUNCIA)) {
            stmt.setLong(1, idDenuncia);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new HistoricoStatus(rs.getLong("cd_historico"), rs.getLong("cd_denuncia"),
                            JdbcUtil.getEnum(rs, "st_anterior", StatusDenuncia.class),
                            JdbcUtil.getEnum(rs, "st_novo", StatusDenuncia.class),
                            JdbcUtil.getEnum(rs, "tp_origem", OrigemStatus.class),
                            JdbcUtil.getDataHora(rs, "dt_alteracao")));
                }
            }
        }
        return lista;
    }

    @Override
    public void excluirPorDenuncia(Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_POR_DENUNCIA)) {
            stmt.setLong(1, idDenuncia);
            stmt.executeUpdate();
        }
    }

    @Override
    public void excluirDoAutor(Long idAutor) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_DO_AUTOR)) {
            stmt.setLong(1, idAutor);
            stmt.executeUpdate();
        }
    }

    private static void preencher(PreparedStatement stmt, HistoricoStatus mudanca) throws SQLException {
        stmt.setLong(1, mudanca.idDenuncia());
        JdbcUtil.setEnum(stmt, 2, mudanca.anterior());
        JdbcUtil.setEnum(stmt, 3, mudanca.nova());
        JdbcUtil.setEnum(stmt, 4, mudanca.origem());
        JdbcUtil.setDataHora(stmt, 5, mudanca.data());
    }
}
