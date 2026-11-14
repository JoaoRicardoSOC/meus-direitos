package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.SemeaduraCidadeDao;
import br.com.fiap.meusdireitos.model.SemeaduraCidade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de {@link SemeaduraCidadeDao}.
 */
public class SemeaduraCidadeDaoJdbc implements SemeaduraCidadeDao {

    private static final String COLUNAS =
            "nm_cidade_chave, sg_uf, nm_cidade, dt_semeadura, dt_ultimo_pulso, qt_eventos";
    private static final String SQL_INSERIR =
            "INSERT INTO tb_gs26s2_semeadura_cidade (nm_cidade_chave, sg_uf, nm_cidade, dt_semeadura, dt_ultimo_pulso) "
                    + "VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_BUSCAR =
            "SELECT " + COLUNAS + " FROM tb_gs26s2_semeadura_cidade WHERE nm_cidade_chave = ? AND sg_uf = ?";
    private static final String SQL_BUSCAR_PARA_ATUALIZAR = SQL_BUSCAR + " FOR UPDATE";
    private static final String SQL_PULSO =
            "UPDATE tb_gs26s2_semeadura_cidade SET dt_ultimo_pulso = ?, qt_eventos = qt_eventos + ? "
                    + "WHERE nm_cidade_chave = ? AND sg_uf = ?";
    private static final String SQL_LISTAR =
            "SELECT " + COLUNAS + " FROM tb_gs26s2_semeadura_cidade ORDER BY dt_semeadura";
    private static final String SQL_EXCLUIR_TODAS = "DELETE FROM tb_gs26s2_semeadura_cidade";

    private final Connection conexao;

    public SemeaduraCidadeDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public boolean registrar(String cidadeChave, String uf, String cidade) throws SQLException {
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            stmt.setString(1, cidadeChave);
            stmt.setString(2, uf);
            stmt.setString(3, cidade);
            JdbcUtil.setDataHora(stmt, 4, agora);
            JdbcUtil.setDataHora(stmt, 5, agora);
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException jaSemeada) {
            return false; // ORA-00001: outra requisição já marcou esta cidade
        }
    }

    @Override
    public SemeaduraCidade buscar(String cidadeChave, String uf) throws SQLException {
        return buscar(SQL_BUSCAR, cidadeChave, uf);
    }

    @Override
    public SemeaduraCidade buscarParaAtualizar(String cidadeChave, String uf) throws SQLException {
        return buscar(SQL_BUSCAR_PARA_ATUALIZAR, cidadeChave, uf);
    }

    private SemeaduraCidade buscar(String sql, String cidadeChave, String uf) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cidadeChave);
            stmt.setString(2, uf);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public void registrarPulso(String cidadeChave, String uf, LocalDateTime quando, int eventos) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_PULSO)) {
            JdbcUtil.setDataHora(stmt, 1, quando);
            stmt.setInt(2, eventos);
            stmt.setString(3, cidadeChave);
            stmt.setString(4, uf);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<SemeaduraCidade> listar() throws SQLException {
        List<SemeaduraCidade> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public void excluirTodas() throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_TODAS)) {
            stmt.executeUpdate();
        }
    }

    private static SemeaduraCidade mapear(ResultSet rs) throws SQLException {
        return new SemeaduraCidade(rs.getString("nm_cidade_chave"), rs.getString("sg_uf"), rs.getString("nm_cidade"),
                JdbcUtil.getDataHora(rs, "dt_semeadura"), JdbcUtil.getDataHora(rs, "dt_ultimo_pulso"),
                rs.getInt("qt_eventos"));
    }
}
