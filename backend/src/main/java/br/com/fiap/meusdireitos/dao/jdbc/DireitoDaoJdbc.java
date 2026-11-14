package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.DireitoDao;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.Direito;
import br.com.fiap.meusdireitos.model.PassoDireito;
import br.com.fiap.meusdireitos.model.enums.IconeDireito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementação JDBC de {@link DireitoDao}.
 * <p>
 * Para listar o catálogo sem fazer uma consulta por tema (problema N+1), carrega os temas,
 * depois todos os passos e todas as tags, e associa cada um ao seu tema com um {@link Map}.
 */
public class DireitoDaoJdbc implements DireitoDao {

    private static final String COLUNAS =
            "d.cd_direito, d.ds_titulo, d.ds_resumo, d.ds_direito, d.ds_aviso, d.ds_base_legal, "
                    + "d.ds_link_oficial, d.tp_icone, d.nr_ordem";
    private static final String ORDENACAO = " ORDER BY d.nr_ordem, d.ds_titulo";

    private static final String SQL_INSERIR =
            "INSERT INTO tb_gs26s2_direito (cd_direito, ds_titulo, ds_resumo, ds_direito, ds_aviso, ds_base_legal, "
                    + "ds_link_oficial, tp_icone, nr_ordem) VALUES (seq_gs26s2_direito.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE tb_gs26s2_direito SET ds_titulo = ?, ds_resumo = ?, ds_direito = ?, ds_aviso = ?, ds_base_legal = ?, "
                    + "ds_link_oficial = ?, tp_icone = ?, nr_ordem = ? WHERE cd_direito = ?";
    private static final String SQL_EXCLUIR = "DELETE FROM tb_gs26s2_direito WHERE cd_direito = ?";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM tb_gs26s2_direito d WHERE d.cd_direito = ?";
    private static final String SQL_LISTAR =
            "SELECT " + COLUNAS + " FROM tb_gs26s2_direito d" + ORDENACAO;
    private static final String SQL_LISTAR_POR_TAG =
            "SELECT " + COLUNAS + " FROM tb_gs26s2_direito d WHERE EXISTS "
                    + "(SELECT 1 FROM tb_gs26s2_direito_tag t WHERE t.cd_direito = d.cd_direito AND t.tp_tag = ?)" + ORDENACAO;
    private static final String SQL_LISTAR_RESUMO_POR_TAGS =
            "SELECT DISTINCT " + COLUNAS + " FROM tb_gs26s2_direito d "
                    + "INNER JOIN tb_gs26s2_direito_tag t ON t.cd_direito = d.cd_direito "
                    + "WHERE t.tp_tag IN (%s)" + ORDENACAO;
    private static final String SQL_BUSCAR_ID_POR_TITULO =
            "SELECT cd_direito FROM tb_gs26s2_direito WHERE UPPER(ds_titulo) = UPPER(?)";

    private static final String SQL_PASSOS_DE_UM =
            "SELECT cd_direito, nr_ordem, ds_titulo, ds_passo FROM tb_gs26s2_direito_passo WHERE cd_direito = ? ORDER BY nr_ordem";
    private static final String SQL_PASSOS_TODOS =
            "SELECT cd_direito, nr_ordem, ds_titulo, ds_passo FROM tb_gs26s2_direito_passo ORDER BY cd_direito, nr_ordem";
    private static final String SQL_INSERIR_PASSO =
            "INSERT INTO tb_gs26s2_direito_passo (cd_direito, nr_ordem, ds_titulo, ds_passo) VALUES (?, ?, ?, ?)";
    private static final String SQL_EXCLUIR_PASSOS = "DELETE FROM tb_gs26s2_direito_passo WHERE cd_direito = ?";

    private static final String SQL_TAGS_DE_UM =
            "SELECT cd_direito, tp_tag FROM tb_gs26s2_direito_tag WHERE cd_direito = ?";
    private static final String SQL_TAGS_TODAS =
            "SELECT cd_direito, tp_tag FROM tb_gs26s2_direito_tag";
    private static final String SQL_INSERIR_TAG =
            "INSERT INTO tb_gs26s2_direito_tag (cd_direito, tp_tag) VALUES (?, ?)";
    private static final String SQL_EXCLUIR_TAGS = "DELETE FROM tb_gs26s2_direito_tag WHERE cd_direito = ?";

    private final Connection conexao;

    public DireitoDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public Direito inserir(Direito direito) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR, new String[]{"cd_direito"})) {
            preencherDados(stmt, direito);
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (chaves.next()) {
                    direito.setId(chaves.getLong(1));
                }
            }
        }
        inserirPassosETags(direito);
        return direito;
    }

    @Override
    public Direito buscarPorId(Long id) throws SQLException {
        Direito direito;
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntidadeNaoEncontradaException("Tema de direitos não encontrado.");
                }
                direito = mapear(rs);
            }
        }
        Map<Long, Direito> porId = Map.of(direito.getId(), direito);
        carregarPassos(porId, SQL_PASSOS_DE_UM, id);
        carregarTags(porId, SQL_TAGS_DE_UM, id);
        return direito;
    }

    @Override
    public List<Direito> listar() throws SQLException {
        return listar(null);
    }

    @Override
    public List<Direito> listar(TagDemografica tag) throws SQLException {
        Map<Long, Direito> porId = new LinkedHashMap<>(); // mantém a ordem do ORDER BY
        try (PreparedStatement stmt = conexao.prepareStatement(tag == null ? SQL_LISTAR : SQL_LISTAR_POR_TAG)) {
            if (tag != null) {
                JdbcUtil.setEnum(stmt, 1, tag);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Direito direito = mapear(rs);
                    porId.put(direito.getId(), direito);
                }
            }
        }
        if (!porId.isEmpty()) {
            carregarPassos(porId, SQL_PASSOS_TODOS, null);
            carregarTags(porId, SQL_TAGS_TODAS, null);
        }
        return new ArrayList<>(porId.values());
    }

    @Override
    public List<Direito> listarResumoPorTags(Set<TagDemografica> tags) throws SQLException {
        Set<TagDemografica> filtro = EnumSet.of(TagDemografica.TODOS);
        filtro.addAll(tags);
        String sql = String.format(SQL_LISTAR_RESUMO_POR_TAGS, JdbcUtil.placeholders(filtro.size()));

        List<Direito> direitos = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            int i = 1;
            for (TagDemografica tag : filtro) {
                JdbcUtil.setEnum(stmt, i++, tag);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    direitos.add(mapear(rs));
                }
            }
        }
        return direitos;
    }

    @Override
    public Long buscarIdPorTitulo(String titulo) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_ID_POR_TITULO)) {
            stmt.setString(1, titulo == null ? null : titulo.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /** Atualiza o tema e substitui todos os passos e tags (apaga e grava de novo). */
    @Override
    public void atualizar(Direito direito) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ATUALIZAR)) {
            int i = preencherDados(stmt, direito);
            stmt.setLong(i, direito.getId());
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Tema de direitos não encontrado para atualização.");
            }
        }
        excluirFilhos(direito.getId());
        inserirPassosETags(direito);
    }

    /** Exclui passos e tags (tabelas filhas) e depois o tema. */
    @Override
    public void excluir(Long id) throws SQLException {
        excluirFilhos(id);
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR)) {
            stmt.setLong(1, id);
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Tema de direitos não encontrado para exclusão.");
            }
        }
    }

    // ---------------------------------------------------------------- auxiliares

    /** Preenche os parâmetros comuns de INSERT e UPDATE; devolve o próximo índice livre. */
    private int preencherDados(PreparedStatement stmt, Direito direito) throws SQLException {
        int i = 1;
        stmt.setString(i++, direito.getTitulo());
        stmt.setString(i++, direito.getResumo());
        stmt.setString(i++, direito.getDescricao());
        stmt.setString(i++, direito.getAviso());
        stmt.setString(i++, direito.getBaseLegal());
        stmt.setString(i++, direito.getLinkOficial());
        JdbcUtil.setEnum(stmt, i++, direito.getIcone());
        stmt.setInt(i++, direito.getOrdem());
        return i;
    }

    private void inserirPassosETags(Direito direito) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR_PASSO)) {
            for (PassoDireito passo : direito.getPassos()) {
                stmt.setLong(1, direito.getId());
                stmt.setInt(2, passo.ordem());
                stmt.setString(3, passo.titulo());
                stmt.setString(4, passo.texto());
                stmt.executeUpdate();
            }
        }
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR_TAG)) {
            for (TagDemografica tag : direito.getTags()) {
                stmt.setLong(1, direito.getId());
                JdbcUtil.setEnum(stmt, 2, tag);
                stmt.executeUpdate();
            }
        }
    }

    private void excluirFilhos(Long id) throws SQLException {
        for (String sql : new String[]{SQL_EXCLUIR_PASSOS, SQL_EXCLUIR_TAGS}) {
            try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        }
    }

    private void carregarPassos(Map<Long, Direito> porId, String sql, Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            if (id != null) {
                stmt.setLong(1, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Direito direito = porId.get(rs.getLong("cd_direito"));
                    if (direito != null) {
                        direito.adicionarPassoCarregado(new PassoDireito(
                                rs.getInt("nr_ordem"), rs.getString("ds_titulo"), rs.getString("ds_passo")));
                    }
                }
            }
        }
    }

    private void carregarTags(Map<Long, Direito> porId, String sql, Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            if (id != null) {
                stmt.setLong(1, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Direito direito = porId.get(rs.getLong("cd_direito"));
                    if (direito != null) {
                        direito.adicionarTag(JdbcUtil.getEnum(rs, "tp_tag", TagDemografica.class));
                    }
                }
            }
        }
    }

    private Direito mapear(ResultSet rs) throws SQLException {
        Direito direito = new Direito();
        direito.setId(rs.getLong("cd_direito"));
        direito.setTitulo(rs.getString("ds_titulo"));
        direito.setResumo(rs.getString("ds_resumo"));
        direito.setDescricao(rs.getString("ds_direito"));
        direito.setAviso(rs.getString("ds_aviso"));
        direito.setBaseLegal(rs.getString("ds_base_legal"));
        direito.setLinkOficial(rs.getString("ds_link_oficial"));
        direito.setIcone(JdbcUtil.getEnum(rs, "tp_icone", IconeDireito.class));
        direito.setOrdem(rs.getInt("nr_ordem"));
        return direito;
    }
}
