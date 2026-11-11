package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.VotoDenunciaDao;
import br.com.fiap.meusdireitos.model.Placar;
import br.com.fiap.meusdireitos.model.VotoDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementação JDBC de {@link VotoDenunciaDao}.
 * <p>
 * "Upsert" sem MERGE: tenta UPDATE; se nenhuma linha mudou, faz INSERT. Se outra requisição do
 * mesmo usuário inserir o voto no meio do caminho, a chave primária composta recusa o INSERT
 * (ORA-00001) e o voto é atualizado — nunca existem dois votos da mesma pessoa.
 */
public class VotoDenunciaDaoJdbc implements VotoDenunciaDao {

    private static final String SQL_ATUALIZAR =
            "UPDATE tb_voto_denuncia SET tp_voto = ?, dt_voto = ? WHERE cd_usuario = ? AND cd_denuncia = ?";
    private static final String SQL_INSERIR =
            "INSERT INTO tb_voto_denuncia (tp_voto, dt_voto, cd_usuario, cd_denuncia) VALUES (?, ?, ?, ?)";
    private static final String SQL_REMOVER =
            "DELETE FROM tb_voto_denuncia WHERE cd_usuario = ? AND cd_denuncia = ?";
    private static final String SQL_EXCLUIR_POR_DENUNCIA =
            "DELETE FROM tb_voto_denuncia WHERE cd_denuncia = ?";
    private static final String SQL_EXCLUIR_DO_USUARIO =
            "DELETE FROM tb_voto_denuncia WHERE cd_usuario = ? "
                    + "OR cd_denuncia IN (SELECT cd_denuncia FROM tb_denuncia WHERE cd_usuario = ?)";
    private static final String SQL_PLACAR =
            "SELECT COUNT(CASE WHEN tp_voto = 'UP' THEN 1 END) AS qt_up, "
                    + "COUNT(CASE WHEN tp_voto = 'DOWN' THEN 1 END) AS qt_down, "
                    + "MAX(CASE WHEN cd_usuario = ? THEN tp_voto END) AS tp_meu_voto "
                    + "FROM tb_voto_denuncia WHERE cd_denuncia = ?";

    private final Connection conexao;

    public VotoDenunciaDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public void salvar(VotoDenuncia voto) throws SQLException {
        if (voto.getDataVoto() == null) {
            voto.setDataVoto(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }
        if (executar(SQL_ATUALIZAR, voto) > 0) {
            return;
        }
        try {
            executar(SQL_INSERIR, voto);
        } catch (SQLIntegrityConstraintViolationException corrida) {
            executar(SQL_ATUALIZAR, voto); // o voto foi criado por outra requisição: atualiza
        }
    }

    @Override
    public void inserirEmLote(List<VotoDenuncia> votos) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            for (VotoDenuncia voto : votos) {
                JdbcUtil.setEnum(stmt, 1, voto.getTipo());
                JdbcUtil.setDataHora(stmt, 2, voto.getDataVoto());
                stmt.setLong(3, voto.getIdUsuario());
                stmt.setLong(4, voto.getIdDenuncia());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean jaVotou(Long idUsuario, Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(
                "SELECT 1 FROM tb_voto_denuncia WHERE cd_usuario = ? AND cd_denuncia = ?")) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idDenuncia);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean remover(Long idUsuario, Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_REMOVER)) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idDenuncia);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Placar placar(Long idDenuncia, Long idLeitor) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_PLACAR)) {
            stmt.setLong(1, idLeitor == null ? -1L : idLeitor);
            stmt.setLong(2, idDenuncia);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next(); // agregação sempre devolve uma linha
                return new Placar(rs.getInt("qt_up"), rs.getInt("qt_down"),
                        JdbcUtil.getEnum(rs, "tp_meu_voto", TipoVoto.class));
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

    @Override
    public void excluirDoUsuario(Long idUsuario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_DO_USUARIO)) {
            stmt.setLong(1, idUsuario);
            stmt.setLong(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    /** UPDATE e INSERT recebem os mesmos parâmetros, na mesma ordem. */
    private int executar(String sql, VotoDenuncia voto) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            JdbcUtil.setEnum(stmt, 1, voto.getTipo());
            JdbcUtil.setDataHora(stmt, 2, voto.getDataVoto());
            stmt.setLong(3, voto.getIdUsuario());
            stmt.setLong(4, voto.getIdDenuncia());
            return stmt.executeUpdate();
        }
    }
}
