package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.DenunciaDetalhada;
import br.com.fiap.meusdireitos.model.Pagina;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.OrdemFeed;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.sql.SQLException;
import java.util.List;

/**
 * Acesso às denúncias (TB_DENUNCIA).
 * <p>
 * Além do CRUD, oferece consultas "detalhadas" que já trazem o placar de votos e o número de
 * comentários (JOIN + GROUP BY), usadas pelas telas. O parâmetro {@code idLeitor} identifica
 * quem está vendo, para informar o próprio voto (null = visitante).
 */
public interface DenunciaDao extends CrudDao<Denuncia> {

    void atualizarStatus(Long id, StatusDenuncia status) throws SQLException;

    /**
     * @throws br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException se não existir
     */
    DenunciaDetalhada buscarDetalhada(Long id, Long idLeitor) throws SQLException;

    /** Listagem com filtros opcionais, da mais recente para a mais antiga. */
    Pagina<DenunciaDetalhada> pesquisar(FiltroDenuncia filtro, Long idLeitor, int pagina, int tamanho)
            throws SQLException;

    /** Todas as denúncias de um autor, da mais recente para a mais antiga. */
    List<DenunciaDetalhada> listarPorAutor(Long idAutor, Long idLeitor) throws SQLException;

    /**
     * Feed da cidade, ordenado conforme {@code ordem}.
     *
     * @param relevantes          categorias que afetam o perfil do leitor (podem ser vazias)
     * @param bonusRelevancia     pontos somados às denúncias de categorias relevantes (PARA_VOCE)
     * @param penalidadeResolvida pontos subtraídos das denúncias resolvidas (PARA_VOCE)
     */
    Pagina<DenunciaDetalhada> feed(String cidade, String uf, Long idLeitor, List<CategoriaDenuncia> relevantes,
                                   OrdemFeed ordem, int bonusRelevancia, int penalidadeResolvida,
                                   int pagina, int tamanho) throws SQLException;
}
