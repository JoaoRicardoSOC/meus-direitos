package br.com.fiap.meusdireitos.dao.jdbc;

import br.com.fiap.meusdireitos.dao.DenunciaDao;
import br.com.fiap.meusdireitos.dao.FiltroDenuncia;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.DenunciaDetalhada;
import br.com.fiap.meusdireitos.model.Pagina;
import br.com.fiap.meusdireitos.model.Placar;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.OrdemFeed;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;
import br.com.fiap.meusdireitos.util.TextoUtil;

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
 * Implementação JDBC de {@link DenunciaDao}.
 * <p>
 * As consultas de tela partem de uma mesma base: denúncia + autor (INNER JOIN) + votos (LEFT JOIN,
 * para incluir denúncias sem voto), agrupadas para contar o placar; os comentários são contados por
 * subconsulta (um segundo LEFT JOIN multiplicaria as linhas e inflaria os votos). Cada tela muda só
 * o filtro (WHERE) e a ordenação; a paginação usa ROWNUM sobre a consulta ordenada.
 */
public class DenunciaDaoJdbc implements DenunciaDao {

    /** Usado no lugar de um leitor nulo: nenhum usuário tem id -1, então "meu voto" fica vazio. */
    private static final long SEM_LEITOR = -1L;
    /** Valor que nunca casa com uma categoria, para um IN vazio. */
    private static final String SEM_CATEGORIA = "__NENHUMA__";

    private static final String COLUNAS =
            "d.cd_denuncia, d.cd_usuario, d.ds_titulo, d.ds_denuncia, d.tp_categoria, d.st_denuncia, "
                    + "d.ds_endereco, d.nm_bairro, d.nm_cidade, d.sg_uf, d.ds_imagem_url, d.dt_criacao, d.dt_atualizacao";

    // ------------------------------------------------------------------ CRUD

    private static final String SQL_INSERIR =
            "INSERT INTO tb_denuncia (cd_denuncia, cd_usuario, ds_titulo, ds_denuncia, tp_categoria, st_denuncia, "
                    + "ds_endereco, nm_bairro, nm_cidade, sg_uf, ds_imagem_url, dt_criacao, dt_atualizacao) "
                    + "VALUES (seq_denuncia.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + ", u.nm_usuario AS nm_autor, u.st_simulado AS st_autor_simulado FROM tb_denuncia d "
                    + "INNER JOIN tb_usuario u ON u.cd_usuario = d.cd_usuario WHERE d.cd_denuncia = ?";
    private static final String SQL_LISTAR =
            "SELECT " + COLUNAS + ", u.nm_usuario AS nm_autor, u.st_simulado AS st_autor_simulado FROM tb_denuncia d "
                    + "INNER JOIN tb_usuario u ON u.cd_usuario = d.cd_usuario ORDER BY d.dt_criacao DESC";
    private static final String SQL_ATUALIZAR =
            "UPDATE tb_denuncia SET ds_titulo = ?, ds_denuncia = ?, tp_categoria = ?, ds_endereco = ?, nm_bairro = ?, "
                    + "ds_imagem_url = ?, dt_atualizacao = ? WHERE cd_denuncia = ?";
    private static final String SQL_ATUALIZAR_STATUS =
            "UPDATE tb_denuncia SET st_denuncia = ?, dt_atualizacao = ? WHERE cd_denuncia = ?";
    private static final String SQL_EXCLUIR =
            "DELETE FROM tb_denuncia WHERE cd_denuncia = ?";
    private static final String SQL_EXCLUIR_DO_AUTOR =
            "DELETE FROM tb_denuncia WHERE cd_usuario = ?";

    // ------------------------------------------------------------------ consultas de tela

    /** %s = colunas extras do feed; %s = WHERE; %s = ORDER BY. */
    private static final String SQL_DETALHADA =
            "SELECT " + COLUNAS + ", u.nm_usuario AS nm_autor, u.st_simulado AS st_autor_simulado, "
                    + "COUNT(CASE WHEN v.tp_voto = 'UP' THEN 1 END) AS qt_up, "
                    + "COUNT(CASE WHEN v.tp_voto = 'DOWN' THEN 1 END) AS qt_down, "
                    + "NVL(SUM(CASE v.tp_voto WHEN 'UP' THEN 1 WHEN 'DOWN' THEN -1 END), 0) AS nr_saldo, "
                    + "MAX(CASE WHEN v.cd_usuario = ? THEN v.tp_voto END) AS tp_meu_voto, "
                    + "(SELECT COUNT(*) FROM tb_comentario c WHERE c.cd_denuncia = d.cd_denuncia) AS qt_comentarios, "
                    + "(SELECT COUNT(*) FROM tb_confirmacao_resolucao r WHERE r.cd_denuncia = d.cd_denuncia) "
                    + "AS qt_confirmacoes, "
                    + "(SELECT COUNT(*) FROM tb_confirmacao_resolucao r WHERE r.cd_denuncia = d.cd_denuncia "
                    + "AND r.cd_usuario = ?) AS st_confirmei"
                    + "%s "
                    + "FROM tb_denuncia d "
                    + "INNER JOIN tb_usuario u ON u.cd_usuario = d.cd_usuario "
                    + "LEFT JOIN tb_voto_denuncia v ON v.cd_denuncia = d.cd_denuncia "
                    + "WHERE %s "
                    + "GROUP BY " + COLUNAS + ", u.nm_usuario, u.st_simulado "
                    + "ORDER BY %s";

    /** Colunas extras do feed: relevância para o leitor. %s = marcadores do IN (duas vezes). */
    private static final String COLUNAS_RELEVANCIA =
            ", CASE WHEN d.tp_categoria IN (%1$s) THEN 1 ELSE 0 END AS st_relevante, "
                    + "NVL(SUM(CASE v.tp_voto WHEN 'UP' THEN 1 WHEN 'DOWN' THEN -1 END), 0) "
                    + "+ CASE WHEN d.tp_categoria IN (%1$s) THEN ? ELSE 0 END "
                    + "- CASE WHEN d.st_denuncia = 'RESOLVIDA' THEN ? ELSE 0 END AS nr_relevancia";

    /** Paginação com ROWNUM sobre a consulta já ordenada. */
    private static final String SQL_PAGINAR =
            "SELECT * FROM (SELECT q.*, ROWNUM AS nr_linha FROM (%s) q WHERE ROWNUM <= ?) WHERE nr_linha > ?";

    private static final String SQL_CONTAR = "SELECT COUNT(*) FROM tb_denuncia d WHERE %s";

    private static final String ORDEM_RECENTES = "d.dt_criacao DESC, d.cd_denuncia DESC";

    private final Connection conexao;

    public DenunciaDaoJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    // ================================================================== CRUD

    @Override
    public Denuncia inserir(Denuncia denuncia) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR, new String[]{"cd_denuncia"})) {
            preencherInsercao(stmt, denuncia);
            stmt.executeUpdate();
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (chaves.next()) {
                    denuncia.setId(chaves.getLong(1));
                }
            }
        }
        return denuncia;
    }

    @Override
    public void inserirEmLote(List<Denuncia> denuncias) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_INSERIR)) {
            for (Denuncia denuncia : denuncias) {
                preencherInsercao(stmt, denuncia);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public Map<String, Long> idsPorAutorETitulo(List<Long> autores) throws SQLException {
        Map<String, Long> ids = new HashMap<>();
        if (autores.isEmpty()) {
            return ids;
        }
        String sql = "SELECT cd_denuncia, cd_usuario, ds_titulo FROM tb_denuncia WHERE cd_usuario IN ("
                + JdbcUtil.placeholders(autores.size()) + ")";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            JdbcUtil.preencher(stmt, new ArrayList<>(autores));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.put(chaveAutorTitulo(rs.getLong("cd_usuario"), rs.getString("ds_titulo")),
                            rs.getLong("cd_denuncia"));
                }
            }
        }
        return ids;
    }

    /** Chave usada em {@link #idsPorAutorETitulo}: "idAutor|título". */
    public static String chaveAutorTitulo(Long idAutor, String titulo) {
        return idAutor + "|" + titulo;
    }

    /** Parâmetros do INSERT (os mesmos na inserção individual e em lote). */
    private static void preencherInsercao(PreparedStatement stmt, Denuncia denuncia) throws SQLException {
        if (denuncia.getDataCriacao() == null) {
            denuncia.setDataCriacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }
        denuncia.setDataAtualizacao(denuncia.getDataCriacao());
        stmt.setLong(1, denuncia.getIdAutor());
        stmt.setString(2, denuncia.getTitulo());
        stmt.setString(3, denuncia.getDescricao());
        JdbcUtil.setEnum(stmt, 4, denuncia.getCategoria());
        JdbcUtil.setEnum(stmt, 5, denuncia.getStatus());
        stmt.setString(6, denuncia.getEndereco());
        stmt.setString(7, denuncia.getBairro());
        stmt.setString(8, denuncia.getCidade());
        stmt.setString(9, denuncia.getUf());
        stmt.setString(10, denuncia.getImagemUrl());
        JdbcUtil.setDataHora(stmt, 11, denuncia.getDataCriacao());
        JdbcUtil.setDataHora(stmt, 12, denuncia.getDataAtualizacao());
    }

    @Override
    public Denuncia buscarPorId(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_BUSCAR_POR_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new EntidadeNaoEncontradaException("Denúncia não encontrada.");
                }
                return mapearDenuncia(rs);
            }
        }
    }

    @Override
    public List<Denuncia> listar() throws SQLException {
        List<Denuncia> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearDenuncia(rs));
            }
        }
        return lista;
    }

    /** Atualiza os dados editáveis pelo autor (cidade, UF, autor e status não mudam por aqui). */
    @Override
    public void atualizar(Denuncia denuncia) throws SQLException {
        denuncia.setDataAtualizacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ATUALIZAR)) {
            stmt.setString(1, denuncia.getTitulo());
            stmt.setString(2, denuncia.getDescricao());
            JdbcUtil.setEnum(stmt, 3, denuncia.getCategoria());
            stmt.setString(4, denuncia.getEndereco());
            stmt.setString(5, denuncia.getBairro());
            stmt.setString(6, denuncia.getImagemUrl());
            JdbcUtil.setDataHora(stmt, 7, denuncia.getDataAtualizacao());
            stmt.setLong(8, denuncia.getId());
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Denúncia não encontrada para atualização.");
            }
        }
    }

    @Override
    public void atualizarStatus(Long id, StatusDenuncia status) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_ATUALIZAR_STATUS)) {
            JdbcUtil.setEnum(stmt, 1, status);
            JdbcUtil.setDataHora(stmt, 2, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            stmt.setLong(3, id);
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Denúncia não encontrada.");
            }
        }
    }

    /** Exclui só a denúncia: comentários e votos devem ser excluídos antes, na mesma transação. */
    @Override
    public void excluir(Long id) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR)) {
            stmt.setLong(1, id);
            if (stmt.executeUpdate() == 0) {
                throw new EntidadeNaoEncontradaException("Denúncia não encontrada para exclusão.");
            }
        }
    }

    @Override
    public List<Denuncia> listarSimuladasDaCidade(String cidade, String uf) throws SQLException {
        String sql = "SELECT " + COLUNAS + ", u.nm_usuario AS nm_autor, u.st_simulado AS st_autor_simulado "
                + "FROM tb_denuncia d INNER JOIN tb_usuario u ON u.cd_usuario = d.cd_usuario "
                + "WHERE u.st_simulado = 'S' AND " + JdbcUtil.cidadeIgual("d.nm_cidade") + " AND d.sg_uf = ? "
                + "ORDER BY d.dt_criacao DESC";
        List<Denuncia> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, TextoUtil.chaveCidade(cidade));
            stmt.setString(2, uf);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearDenuncia(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public int excluirDoAutor(Long idAutor) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(SQL_EXCLUIR_DO_AUTOR)) {
            stmt.setLong(1, idAutor);
            return stmt.executeUpdate();
        }
    }

    // ================================================================== consultas de tela

    @Override
    public DenunciaDetalhada buscarDetalhada(Long id, Long idLeitor) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        adicionarLeitor(parametros, idLeitor);
        parametros.add(id);
        String sql = String.format(SQL_DETALHADA, "", "d.cd_denuncia = ?", ORDEM_RECENTES);
        List<DenunciaDetalhada> resultado = executarConsulta(sql, parametros, false);
        if (resultado.isEmpty()) {
            throw new EntidadeNaoEncontradaException("Denúncia não encontrada.");
        }
        return resultado.get(0);
    }

    @Override
    public List<DenunciaDetalhada> listarPorAutor(Long idAutor, Long idLeitor) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        adicionarLeitor(parametros, idLeitor);
        parametros.add(idAutor);
        String sql = String.format(SQL_DETALHADA, "", "d.cd_usuario = ?", ORDEM_RECENTES);
        return executarConsulta(sql, parametros, false);
    }

    @Override
    public Pagina<DenunciaDetalhada> pesquisar(FiltroDenuncia filtro, Long idLeitor, int pagina, int tamanho)
            throws SQLException {
        List<Object> parametrosWhere = new ArrayList<>();
        String where = montarWhere(filtro, parametrosWhere);

        List<Object> parametros = new ArrayList<>();
        adicionarLeitor(parametros, idLeitor);
        parametros.addAll(parametrosWhere);
        String sql = String.format(SQL_DETALHADA, "", where, ORDEM_RECENTES);

        List<DenunciaDetalhada> itens = executarPaginada(sql, parametros, pagina, tamanho, false);
        return new Pagina<>(pagina, tamanho, contar(where, parametrosWhere), itens);
    }

    @Override
    public Pagina<DenunciaDetalhada> feed(String cidade, String uf, Long idLeitor, List<CategoriaDenuncia> relevantes,
                                          OrdemFeed ordem, int bonusRelevancia, int penalidadeResolvida,
                                          int pagina, int tamanho) throws SQLException {
        List<Object> categorias = new ArrayList<>(relevantes);
        if (categorias.isEmpty()) {
            categorias.add(SEM_CATEGORIA);
        }
        String extras = String.format(COLUNAS_RELEVANCIA, JdbcUtil.placeholders(categorias.size()));
        String where = JdbcUtil.cidadeIgual("d.nm_cidade") + " AND d.sg_uf = ?";

        // a ordem dos parâmetros segue a ordem dos "?" no SQL: SELECT (leitor, extras) → WHERE
        List<Object> parametros = new ArrayList<>();
        adicionarLeitor(parametros, idLeitor);
        parametros.addAll(categorias);         // st_relevante
        parametros.addAll(categorias);         // nr_relevancia
        parametros.add(bonusRelevancia);
        parametros.add(penalidadeResolvida);
        parametros.add(TextoUtil.chaveCidade(cidade));
        parametros.add(uf);

        String sql = String.format(SQL_DETALHADA, extras, where, ordenacao(ordem));
        List<DenunciaDetalhada> itens = executarPaginada(sql, parametros, pagina, tamanho, true);
        return new Pagina<>(pagina, tamanho, contar(where, List.of(TextoUtil.chaveCidade(cidade), uf)), itens);
    }

    // ================================================================== auxiliares

    /** A ordenação vem de texto fixo no código (nunca do cliente): sem risco de SQL injection. */
    private static String ordenacao(OrdemFeed ordem) {
        switch (ordem) {
            case PARA_VOCE:
                return "nr_relevancia DESC, " + ORDEM_RECENTES;
            case MAIS_VOTADOS:
                return "nr_saldo DESC, " + ORDEM_RECENTES;
            case RECENTES:
            default:
                return ORDEM_RECENTES;
        }
    }

    private static String montarWhere(FiltroDenuncia filtro, List<Object> parametros) {
        List<String> condicoes = new ArrayList<>();
        condicoes.add("1 = 1");
        if (filtro.cidade() != null) {
            condicoes.add(JdbcUtil.cidadeIgual("d.nm_cidade"));
            parametros.add(TextoUtil.chaveCidade(filtro.cidade()));
        }
        if (filtro.uf() != null) {
            condicoes.add("d.sg_uf = ?");
            parametros.add(filtro.uf().trim().toUpperCase());
        }
        if (filtro.categoria() != null) {
            condicoes.add("d.tp_categoria = ?");
            parametros.add(filtro.categoria());
        }
        if (filtro.status() != null) {
            condicoes.add("d.st_denuncia = ?");
            parametros.add(filtro.status());
        }
        if (filtro.idAutor() != null) {
            condicoes.add("d.cd_usuario = ?");
            parametros.add(filtro.idAutor());
        }
        if (filtro.busca() != null && !filtro.busca().isBlank()) {
            condicoes.add("(UPPER(d.ds_titulo) LIKE ? ESCAPE '\\' OR UPPER(d.ds_denuncia) LIKE ? ESCAPE '\\')");
            String padrao = "%" + escaparLike(filtro.busca().trim().toUpperCase()) + "%";
            parametros.add(padrao);
            parametros.add(padrao);
        }
        return String.join(" AND ", condicoes);
    }

    /** Faz % e _ digitados pelo usuário serem procurados literalmente no LIKE. */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private int contar(String where, List<Object> parametros) throws SQLException {
        try (PreparedStatement stmt = conexao.prepareStatement(String.format(SQL_CONTAR, where))) {
            JdbcUtil.preencher(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<DenunciaDetalhada> executarPaginada(String sql, List<Object> parametros, int pagina, int tamanho,
                                                     boolean comRelevancia) throws SQLException {
        List<Object> todos = new ArrayList<>(parametros);
        todos.add(pagina * tamanho);         // fim
        todos.add((pagina - 1) * tamanho);   // início
        return executarConsulta(String.format(SQL_PAGINAR, sql), todos, comRelevancia);
    }

    private List<DenunciaDetalhada> executarConsulta(String sql, List<Object> parametros, boolean comRelevancia)
            throws SQLException {
        List<DenunciaDetalhada> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            JdbcUtil.preencher(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Placar placar = new Placar(rs.getInt("qt_up"), rs.getInt("qt_down"),
                            JdbcUtil.getEnum(rs, "tp_meu_voto", TipoVoto.class));
                    Boolean relevante = comRelevancia ? rs.getInt("st_relevante") == 1 : null;
                    lista.add(new DenunciaDetalhada(mapearDenuncia(rs), placar, rs.getInt("qt_comentarios"),
                            rs.getInt("qt_confirmacoes"), rs.getInt("st_confirmei") > 0, relevante));
                }
            }
        }
        return lista;
    }

    /** O leitor aparece duas vezes no SELECT base: no "meu voto" e no "eu confirmei". */
    private static void adicionarLeitor(List<Object> parametros, Long idLeitor) {
        long leitor = idLeitor == null ? SEM_LEITOR : idLeitor;
        parametros.add(leitor);
        parametros.add(leitor);
    }

    private static Denuncia mapearDenuncia(ResultSet rs) throws SQLException {
        Denuncia d = new Denuncia();
        d.setId(rs.getLong("cd_denuncia"));
        d.setIdAutor(rs.getLong("cd_usuario"));
        d.setNomeAutor(rs.getString("nm_autor"));
        d.setAutorSimulado("S".equals(rs.getString("st_autor_simulado")));
        d.setTitulo(rs.getString("ds_titulo"));
        d.setDescricao(rs.getString("ds_denuncia"));
        d.setCategoria(JdbcUtil.getEnum(rs, "tp_categoria", CategoriaDenuncia.class));
        d.setStatus(JdbcUtil.getEnum(rs, "st_denuncia", StatusDenuncia.class));
        d.setEndereco(rs.getString("ds_endereco"));
        d.setBairro(rs.getString("nm_bairro"));
        d.setCidade(rs.getString("nm_cidade"));
        d.setUf(rs.getString("sg_uf"));
        d.setImagemUrl(rs.getString("ds_imagem_url"));
        d.setDataCriacao(JdbcUtil.getDataHora(rs, "dt_criacao"));
        d.setDataAtualizacao(JdbcUtil.getDataHora(rs, "dt_atualizacao"));
        return d;
    }
}
