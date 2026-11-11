package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Direito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Acesso ao catálogo de direitos: TB_DIREITO + TB_DIREITO_PASSO + TB_DIREITO_TAG.
 * Gravar, atualizar e excluir mexem nas três tabelas e devem rodar dentro de uma transação.
 */
public interface DireitoDao extends CrudDao<Direito> {

    /**
     * @param tag filtro opcional (null = todos)
     * @return temas completos (com passos e tags), ordenados por ordem e título
     */
    List<Direito> listar(TagDemografica tag) throws SQLException;

    /**
     * Temas que valem para alguma das tags informadas (ou para TODOS).
     * Carrega apenas os dados do card (sem passos), para a tela "Meus Direitos".
     */
    List<Direito> listarResumoPorTags(Set<TagDemografica> tags) throws SQLException;

    /**
     * @return id do tema com esse título (sem diferenciar maiúsculas), ou null se não existir
     */
    Long buscarIdPorTitulo(String titulo) throws SQLException;
}
