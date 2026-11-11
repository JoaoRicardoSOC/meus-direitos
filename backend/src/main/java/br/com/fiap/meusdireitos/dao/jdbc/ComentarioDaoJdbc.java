package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.ComentarioDao;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.Comentario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de {@link ComentarioDao}. As leituras trazem o nome do autor (INNER JOIN).
 */
public class ComentarioDaoJdbc implements ComentarioDao {

    private static final String SELECT =
            "SELECT c.cd_comentario, c.cd_denuncia, c.cd_usuario, u.nm_usuario, c.ds_comentario, c.dt_criacao, "
                    + "c.dt_edicao FROM tb_comentario c INNER JOIN tb_usuario u ON u.cd_usuario = c.cd_usuario ";

    private static final String SQL_INSERIR =
            "INSERT INTO tb_comentario (cd_comentario, cd_denuncia, cd_usuario, ds_comentario, dt_criacao) "
                    + "VALUES (seq_comentario.NEXTVAL, ?, ?, ?, ?)";
    private static final String SQL_BUSCAR_POR_ID = SELECT + "WHERE c.cd_comentario = ?";
    private static final String SQL_LISTAR = SELECT + "ORDER BY c.dt_criacao, c.cd_comentario";
    private static final String SQL_LISTAR_POR_DENUNCIA =
            SELECT + "WHERE c.cd_denuncia = ? ORDER BY c.dt_criacao, c.cd_comentario";
    private static final String SQL_ATUALIZAR =
            "UPDATE tb_comentario SET ds_comentario = ?, dt_edicao = ? WHERE cd_comentario = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM tb_comentario WHERE cd_comentario = ?";
    private static final String SQL_EXCLUIR_POR_DENUNCIA = "DELETE FROM tb_comentario WHERE cd_denuncia = ?";

    private final Connection conexao;

    public ComentarioDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public Comentario inserir(Comentario comentario) throws SQLException {
        if (comentario.getDataCriacao() == null) {
            comentario.setDataCriacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR, new String[]{"cd_comentario"})) {
            stmt.setLong(1, comentario.getIdDenuncia());
            stmt.setLong(2, comentario.getIdAutor());
            stmt.setString(3, comentario.getTexto());
            JdbcUtil.setDataHora(stmt, 4, comentario.getDataCriacao());
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (chaves.next()) {
                    comentario.setId(chaves.getLong(1));
                }
            }
        }
        return comentario;
    }

    @Override
    public Comentario buscarPorId(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntidadeNaoEncontradaException("Comentário não encontrado.");
                }
                return mapear(rs);
            }
        }
    }

    @Override
    public List<Comentario> listar() throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR)) {
            return mapearTodos(stmt);
        }
    }

    @Override
    public List<Comentario> listarPorDenuncia(Long idDenuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR_POR_DENUNCIA)) {
            stmt.setLong(1, idDenuncia);
            return mapearTodos(stmt);
        }
    }

    /** Atualiza o texto e registra a data da edição. */
    @Override
    public void atualizar(Comentario comentario) throws SQLException {
        comentario.setDataEdicao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ATUALIZAR)) {
            stmt.setString(1, comentario.getTexto());
            JdbcUtil.setDataHora(stmt, 2, comentario.getDataEdicao());
            stmt.setLong(3, comentario.getId());
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Comentário não encontrado para atualização.");
            }
        }
    }

    @Override
    public void excluir(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR)) {
            stmt.setLong(1, id);
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Comentário não encontrado para exclusão.");
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

    private List<Comentario> mapearTodos(PreparedStatement stmt) throws SQLException {
        List<Comentario> lista = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private static Comentario mapear(ResultSet rs) throws SQLException {
        Comentario c = new Comentario();
        c.setId(rs.getLong("cd_comentario"));
        c.setIdDenuncia(rs.getLong("cd_denuncia"));
        c.setIdAutor(rs.getLong("cd_usuario"));
        c.setNomeAutor(rs.getString("nm_usuario"));
        c.setTexto(rs.getString("ds_comentario"));
        c.setDataCriacao(JdbcUtil.getDataHora(rs, "dt_criacao"));
        c.setDataEdicao(JdbcUtil.getDataHora(rs, "dt_edicao"));
        return c;
    }
}
