package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.UsuarioDao;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação JDBC de {@link UsuarioDao}.
 * A conexão é recebida pronta (aberta e controlada pela {@code Transacao} no service).
 */
public class UsuarioDaoJdbc implements UsuarioDao {

    private static final String COLUNAS =
            "cd_usuario, nm_usuario, ds_email, ds_senha_hash, nm_cidade, sg_uf, dt_cadastro, st_simulado";

    private static final String SQL_INSERIR =
            "INSERT INTO tb_usuario (cd_usuario, nm_usuario, ds_email, ds_senha_hash, nm_cidade, sg_uf, dt_cadastro, "
                    + "st_simulado) VALUES (seq_usuario.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR_REAIS =
            "SELECT " + COLUNAS + " FROM tb_usuario WHERE st_simulado = 'N' ORDER BY nm_usuario";
    private static final String SQL_LISTAR_SIMULADOS =
            "SELECT " + COLUNAS + " FROM tb_usuario WHERE st_simulado = 'S' ORDER BY cd_usuario";
    private static final String SQL_LISTAR_SIMULADOS_DA_CIDADE =
            "SELECT " + COLUNAS + " FROM tb_usuario WHERE st_simulado = 'S' "
                    + "AND UPPER(nm_cidade) = UPPER(?) AND sg_uf = ? ORDER BY cd_usuario";
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM tb_usuario WHERE cd_usuario = ?";
    private static final String SQL_BUSCAR_POR_EMAIL =
            "SELECT " + COLUNAS + " FROM tb_usuario WHERE ds_email = ?";
    private static final String SQL_LISTAR =
            "SELECT " + COLUNAS + " FROM tb_usuario ORDER BY nm_usuario";
    private static final String SQL_ATUALIZAR =
            "UPDATE tb_usuario SET nm_usuario = ?, ds_email = ?, ds_senha_hash = ?, nm_cidade = ?, sg_uf = ? "
                    + "WHERE cd_usuario = ?";
    private static final String SQL_EXCLUIR =
            "DELETE FROM tb_usuario WHERE cd_usuario = ?";
    private static final String SQL_EXISTE =
            "SELECT 1 FROM tb_usuario WHERE cd_usuario = ?";
    private static final String SQL_CONTAR_DENUNCIAS =
            "SELECT COUNT(*) FROM tb_denuncia WHERE cd_usuario = ?";

    private final Connection conexao;

    public UsuarioDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public Usuario inserir(Usuario usuario) throws SQLException {
        LocalDateTime agora = usuario.getDataCadastro() != null
                ? usuario.getDataCadastro() : LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR, new String[]{"cd_usuario"})) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenhaHash());
            stmt.setString(4, usuario.getCidade());
            stmt.setString(5, usuario.getUf());
            JdbcUtil.setDataHora(stmt, 6, agora);
            JdbcUtil.setSimNao(stmt, 7, usuario.isSimulado());
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (chaves.next()) {
                    usuario.setId(chaves.getLong(1));
                }
            }
        }
        usuario.setDataCadastro(agora);
        return usuario;
    }

    @Override
    public void inserirEmLote(List<Usuario> usuarios) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            for (Usuario usuario : usuarios) {
                if (usuario.getDataCadastro() == null) {
                    usuario.setDataCadastro(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                }
                stmt.setString(1, usuario.getNome());
                stmt.setString(2, usuario.getEmail());
                stmt.setString(3, usuario.getSenhaHash());
                stmt.setString(4, usuario.getCidade());
                stmt.setString(5, usuario.getUf());
                JdbcUtil.setDataHora(stmt, 6, usuario.getDataCadastro());
                JdbcUtil.setSimNao(stmt, 7, usuario.isSimulado());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public Map<String, Long> idsPorEmail(List<String> emails) throws SQLException {
        Map<String, Long> ids = new HashMap<>();
        if (emails.isEmpty()) {
            return ids;
        }
        String sql = "SELECT cd_usuario, ds_email FROM tb_usuario WHERE ds_email IN ("
                + JdbcUtil.placeholders(emails.size()) + ")";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            JdbcUtil.preencher(stmt, new ArrayList<>(emails));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.put(rs.getString("ds_email"), rs.getLong("cd_usuario"));
                }
            }
        }
        return ids;
    }

    @Override
    public Usuario buscarPorId(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntidadeNaoEncontradaException("Usuário não encontrado.");
                }
                return mapear(rs);
            }
        }
    }

    @Override
    public Usuario buscarPorEmail(String email) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_EMAIL)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Usuario> listar() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        }
        return usuarios;
    }

    @Override
    public List<Usuario> listarReais() throws SQLException {
        return listarCom(SQL_LISTAR_REAIS);
    }

    @Override
    public List<Usuario> listarSimulados() throws SQLException {
        return listarCom(SQL_LISTAR_SIMULADOS);
    }

    @Override
    public List<Usuario> listarSimuladosDaCidade(String cidade, String uf) throws SQLException {
        return listarCom(SQL_LISTAR_SIMULADOS_DA_CIDADE, cidade, uf);
    }

    private List<Usuario> listarCom(String sql, String... parametros) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setString(i + 1, parametros[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapear(rs));
                }
            }
        }
        return usuarios;
    }

    @Override
    public void atualizar(Usuario usuario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ATUALIZAR)) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenhaHash());
            stmt.setString(4, usuario.getCidade());
            stmt.setString(5, usuario.getUf());
            stmt.setLong(6, usuario.getId());
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Usuário não encontrado para atualização.");
            }
        }
    }

    @Override
    public void excluir(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR)) {
            stmt.setLong(1, id);
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Usuário não encontrado para exclusão.");
            }
        }
    }

    @Override
    public boolean existe(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXISTE)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int contarDenuncias(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_CONTAR_DENUNCIAS)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario(
                rs.getLong("cd_usuario"),
                rs.getString("nm_usuario"),
                rs.getString("ds_email"),
                rs.getString("ds_senha_hash"),
                rs.getString("nm_cidade"),
                rs.getString("sg_uf"),
                JdbcUtil.getDataHora(rs, "dt_cadastro"));
        usuario.setSimulado(Boolean.TRUE.equals(JdbcUtil.getSimNao(rs, "st_simulado")));
        return usuario;
    }
}
