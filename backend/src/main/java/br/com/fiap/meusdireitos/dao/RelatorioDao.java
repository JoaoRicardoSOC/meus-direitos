package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.relatorio.DenunciaEngajamento;
import br.com.fiap.meusdireitos.model.relatorio.ResumoCidade;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorBairro;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorCategoria;

import java.sql.SQLException;
import java.util.List;

/**
 * Relatórios da cidade gerados com SQL (JOIN, GROUP BY, HAVING, CASE, subconsultas).
 * Todas as consultas recebem a cidade e a UF.
 */
public interface RelatorioDao {

    ResumoCidade resumo(String cidade, String uf) throws SQLException;

    /** @return categorias com denúncias, da mais para a menos frequente */
    List<TotalPorCategoria> porCategoria(String cidade, String uf) throws SQLException;

    /**
     * @param minimo só bairros com pelo menos esta quantidade de denúncias (HAVING)
     */
    List<TotalPorBairro> porBairro(String cidade, String uf, int minimo) throws SQLException;

    /** @return as denúncias com mais votos e comentários (até {@code limite}) */
    List<DenunciaEngajamento> engajamento(String cidade, String uf, int limite) throws SQLException;
}
