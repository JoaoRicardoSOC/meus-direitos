package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.RelatorioDao;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.relatorio.DenunciaEngajamento;
import br.com.fiap.meusdireitos.model.relatorio.ResumoCidade;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorBairro;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorCategoria;
import br.com.fiap.meusdireitos.util.TextoUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JDBC de {@link RelatorioDao}. Todo o cálculo (contagens, somas, agrupamentos,
 * filtros de grupo e ranking) é feito pelo Oracle; o Java só lê os resultados.
 */
public class RelatorioDaoJdbc implements RelatorioDao {

    /** Filtro de cidade usado em todas as consultas (sem diferenciar maiúsculas nem acentos). */
    private static final String DA_CIDADE = JdbcUtil.cidadeIgual("d.nm_cidade") + " AND d.sg_uf = ?";

    /** Indicadores gerais: várias subconsultas escalares em uma única linha (FROM dual). */
    private static final String SQL_RESUMO =
            "SELECT "
                    + "(SELECT COUNT(*) FROM tb_usuario u WHERE " + JdbcUtil.cidadeIgual("u.nm_cidade") + " AND u.sg_uf = ?) AS qt_usuarios, "
                    + "(SELECT COUNT(*) FROM tb_denuncia d WHERE " + DA_CIDADE + ") AS qt_denuncias, "
                    + "(SELECT COUNT(*) FROM tb_denuncia d WHERE " + DA_CIDADE + " AND d.st_denuncia = 'ABERTA') AS qt_abertas, "
                    + "(SELECT COUNT(*) FROM tb_denuncia d WHERE " + DA_CIDADE + " AND d.st_denuncia = 'EM_ANDAMENTO') AS qt_andamento, "
                    + "(SELECT COUNT(*) FROM tb_denuncia d WHERE " + DA_CIDADE + " AND d.st_denuncia = 'RESOLVIDA') AS qt_resolvidas, "
                    + "(SELECT COUNT(*) FROM tb_voto_denuncia v INNER JOIN tb_denuncia d ON d.cd_denuncia = v.cd_denuncia "
                    + "  WHERE " + DA_CIDADE + ") AS qt_votos, "
                    + "(SELECT COUNT(*) FROM tb_comentario c INNER JOIN tb_denuncia d ON d.cd_denuncia = c.cd_denuncia "
                    + "  WHERE " + DA_CIDADE + ") AS qt_comentarios, "
                    + "(SELECT COUNT(*) FROM tb_confirmacao_resolucao r INNER JOIN tb_denuncia d ON d.cd_denuncia = r.cd_denuncia "
                    + "  WHERE " + DA_CIDADE + ") AS qt_confirmacoes, "
                    + "(SELECT COUNT(*) FROM tb_denuncia d INNER JOIN tb_usuario u ON u.cd_usuario = d.cd_usuario "
                    + "  WHERE " + DA_CIDADE + " AND u.st_simulado = 'S') AS qt_simuladas, "
                    + "(SELECT COUNT(*) FROM tb_direito) AS qt_temas "
                    + "FROM dual";
    /** Quantas vezes o par (cidade, UF) aparece no SQL_RESUMO. */
    private static final int OCORRENCIAS_CIDADE_RESUMO = 9;

    /** Contagem por categoria e situação: GROUP BY + SUM com CASE. */
    private static final String SQL_POR_CATEGORIA =
            "SELECT d.tp_categoria, COUNT(*) AS qt_total, "
                    + "SUM(CASE WHEN d.st_denuncia = 'ABERTA' THEN 1 ELSE 0 END) AS qt_abertas, "
                    + "SUM(CASE WHEN d.st_denuncia = 'EM_ANDAMENTO' THEN 1 ELSE 0 END) AS qt_andamento, "
                    + "SUM(CASE WHEN d.st_denuncia = 'RESOLVIDA' THEN 1 ELSE 0 END) AS qt_resolvidas "
                    + "FROM tb_denuncia d WHERE " + DA_CIDADE + " "
                    + "GROUP BY d.tp_categoria "
                    + "ORDER BY qt_total DESC, d.tp_categoria";

    /** Concentração por bairro: LEFT JOIN com votos + GROUP BY + HAVING. */
    private static final String SQL_POR_BAIRRO =
            "SELECT NVL(d.nm_bairro, 'Não informado') AS nm_bairro, "
                    + "COUNT(DISTINCT d.cd_denuncia) AS qt_total, "
                    + "COUNT(DISTINCT CASE WHEN d.st_denuncia = 'RESOLVIDA' THEN d.cd_denuncia END) AS qt_resolvidas, "
                    + "NVL(SUM(CASE v.tp_voto WHEN 'UP' THEN 1 WHEN 'DOWN' THEN -1 END), 0) AS nr_saldo "
                    + "FROM tb_denuncia d LEFT JOIN tb_voto_denuncia v ON v.cd_denuncia = d.cd_denuncia "
                    + "WHERE " + DA_CIDADE + " "
                    + "GROUP BY NVL(d.nm_bairro, 'Não informado') "
                    + "HAVING COUNT(DISTINCT d.cd_denuncia) >= ? "
                    + "ORDER BY qt_total DESC, nr_saldo DESC";

    /** Ranking de engajamento: agregação na consulta interna, ordenação no meio e ROWNUM por fora. */
    private static final String SQL_ENGAJAMENTO =
            "SELECT * FROM ("
                    + " SELECT x.* FROM ("
                    + "  SELECT d.cd_denuncia, d.ds_titulo, d.tp_categoria, d.st_denuncia, d.dt_criacao, "
                    + "  COUNT(v.cd_usuario) AS qt_votos, "
                    + "  NVL(SUM(CASE v.tp_voto WHEN 'UP' THEN 1 WHEN 'DOWN' THEN -1 END), 0) AS nr_saldo, "
                    + "  (SELECT COUNT(*) FROM tb_comentario c WHERE c.cd_denuncia = d.cd_denuncia) AS qt_comentarios "
                    + "  FROM tb_denuncia d LEFT JOIN tb_voto_denuncia v ON v.cd_denuncia = d.cd_denuncia "
                    + "  WHERE " + DA_CIDADE + " "
                    + "  GROUP BY d.cd_denuncia, d.ds_titulo, d.tp_categoria, d.st_denuncia, d.dt_criacao"
                    + " ) x ORDER BY x.qt_votos + x.qt_comentarios DESC, x.dt_criacao DESC"
                    + ") WHERE ROWNUM <= ?";

    private final Connection conexao;

    public RelatorioDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    @Override
    public ResumoCidade resumo(String cidade, String uf) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_RESUMO)) {
            int i = 1;
            for (int n = 0; n < OCORRENCIAS_CIDADE_RESUMO; n++) {
                stmt.setString(i++, TextoUtil.chaveCidade(cidade));
                stmt.setString(i++, uf);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return new ResumoCidade(rs.getInt("qt_usuarios"), rs.getInt("qt_denuncias"), rs.getInt("qt_abertas"),
                        rs.getInt("qt_andamento"), rs.getInt("qt_resolvidas"), rs.getInt("qt_votos"),
                        rs.getInt("qt_comentarios"), rs.getInt("qt_confirmacoes"), rs.getInt("qt_temas"),
                        rs.getInt("qt_simuladas"));
            }
        }
    }

    @Override
    public List<TotalPorCategoria> porCategoria(String cidade, String uf) throws SQLException {
        List<TotalPorCategoria> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_POR_CATEGORIA)) {
            stmt.setString(1, TextoUtil.chaveCidade(cidade));
            stmt.setString(2, uf);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new TotalPorCategoria(JdbcUtil.getEnum(rs, "tp_categoria", CategoriaDenuncia.class),
                            rs.getInt("qt_total"), rs.getInt("qt_abertas"), rs.getInt("qt_andamento"),
                            rs.getInt("qt_resolvidas")));
                }
            }
        }
        return lista;
    }

    @Override
    public List<TotalPorBairro> porBairro(String cidade, String uf, int minimo) throws SQLException {
        List<TotalPorBairro> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_POR_BAIRRO)) {
            stmt.setString(1, TextoUtil.chaveCidade(cidade));
            stmt.setString(2, uf);
            stmt.setInt(3, minimo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new TotalPorBairro(rs.getString("nm_bairro"), rs.getInt("qt_total"),
                            rs.getInt("qt_resolvidas"), rs.getInt("nr_saldo")));
                }
            }
        }
        return lista;
    }

    @Override
    public List<DenunciaEngajamento> engajamento(String cidade, String uf, int limite) throws SQLException {
        List<DenunciaEngajamento> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ENGAJAMENTO)) {
            stmt.setString(1, TextoUtil.chaveCidade(cidade));
            stmt.setString(2, uf);
            stmt.setInt(3, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new DenunciaEngajamento(rs.getLong("cd_denuncia"), rs.getString("ds_titulo"),
                            JdbcUtil.getEnum(rs, "tp_categoria", CategoriaDenuncia.class),
                            JdbcUtil.getEnum(rs, "st_denuncia", StatusDenuncia.class),
                            rs.getInt("qt_votos"), rs.getInt("nr_saldo"), rs.getInt("qt_comentarios")));
                }
            }
        }
        return lista;
    }
}
