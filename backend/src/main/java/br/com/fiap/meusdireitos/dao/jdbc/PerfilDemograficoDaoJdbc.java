package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.PerfilDemograficoDao;
import br.com.fiap.meusdireitos.model.PerfilDemografico;
import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementação JDBC de {@link PerfilDemograficoDao}.
 * O "upsert" é feito com UPDATE e, se nenhuma linha mudar, INSERT (MERGE não faz parte do conteúdo).
 */
public class PerfilDemograficoDaoJdbc implements PerfilDemograficoDao {

    private static final String SQL_BUSCAR =
            "SELECT cd_usuario, dt_nascimento, tp_raca_cor, tp_deficiencia, tp_percurso_escolar, st_baixa_renda, "
                    + "dt_atualizacao FROM tb_gs26s2_perfil_demografico WHERE cd_usuario = ?";
    private static final String SQL_EXISTE =
            "SELECT 1 FROM tb_gs26s2_perfil_demografico WHERE cd_usuario = ?";
    private static final String SQL_ATUALIZAR =
            "UPDATE tb_gs26s2_perfil_demografico SET dt_nascimento = ?, tp_raca_cor = ?, tp_deficiencia = ?, "
                    + "tp_percurso_escolar = ?, st_baixa_renda = ?, dt_atualizacao = ? WHERE cd_usuario = ?";
    private static final String SQL_INSERIR =
            "INSERT INTO tb_gs26s2_perfil_demografico (dt_nascimento, tp_raca_cor, tp_deficiencia, tp_percurso_escolar, "
                    + "st_baixa_renda, dt_atualizacao, cd_usuario) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_EXCLUIR =
            "DELETE FROM tb_gs26s2_perfil_demografico WHERE cd_usuario = ?";

    private final Connection conexao;

    public PerfilDemograficoDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public PerfilDemografico buscar(Long idUsuario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR)) {
            stmt.setLong(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                PerfilDemografico perfil = new PerfilDemografico();
                perfil.setIdUsuario(rs.getLong("cd_usuario"));
                perfil.setDataNascimento(JdbcUtil.getData(rs, "dt_nascimento"));
                perfil.setRacaCor(JdbcUtil.getEnum(rs, "tp_raca_cor", RacaCor.class));
                perfil.setDeficiencia(JdbcUtil.getEnum(rs, "tp_deficiencia", Deficiencia.class));
                perfil.setPercursoEscolar(JdbcUtil.getEnum(rs, "tp_percurso_escolar", PercursoEscolar.class));
                perfil.setBaixaRenda(JdbcUtil.getSimNao(rs, "st_baixa_renda"));
                perfil.setDataAtualizacao(JdbcUtil.getDataHora(rs, "dt_atualizacao"));
                return perfil;
            }
        }
    }

    @Override
    public boolean existe(Long idUsuario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXISTE)) {
            stmt.setLong(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void salvar(PerfilDemografico perfil) throws SQLException {
        perfil.setDataAtualizacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        if (executar(SQL_ATUALIZAR, perfil) == 0) {
            executar(SQL_INSERIR, perfil);
        }
    }

    @Override
    public boolean excluir(Long idUsuario) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR)) {
            stmt.setLong(1, idUsuario);
            return stmt.executeUpdate() > 0;
        }
    }

    /** UPDATE e INSERT recebem os mesmos parâmetros, na mesma ordem (id do usuário por último). */
    private int executar(String sql, PerfilDemografico perfil) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            JdbcUtil.setData(stmt, 1, perfil.getDataNascimento());
            JdbcUtil.setEnum(stmt, 2, perfil.getRacaCor());
            JdbcUtil.setEnum(stmt, 3, perfil.getDeficiencia());
            JdbcUtil.setEnum(stmt, 4, perfil.getPercursoEscolar());
            JdbcUtil.setSimNao(stmt, 5, perfil.getBaixaRenda());
            JdbcUtil.setDataHora(stmt, 6, perfil.getDataAtualizacao());
            stmt.setLong(7, perfil.getIdUsuario());
            return stmt.executeUpdate();
        }
    }
}
