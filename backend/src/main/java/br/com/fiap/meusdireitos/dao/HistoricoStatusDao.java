package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.HistoricoStatus;
import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.sql.SQLException;
import java.util.List;

/**
 * Linha do tempo das denúncias (TB_GS26S2_HISTORICO_STATUS): cada mudança de situação.
 */
public interface HistoricoStatusDao {

    /** Grava uma mudança agora. */
    void registrar(Long idDenuncia, StatusDenuncia anterior, StatusDenuncia nova, OrigemStatus origem)
            throws SQLException;

    /** Grava várias mudanças de uma vez (lote JDBC), para o conteúdo de demonstração. */
    void registrarEmLote(List<HistoricoStatus> mudancas) throws SQLException;

    /** @return mudanças da denúncia, da mais antiga para a mais nova */
    List<HistoricoStatus> listarPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove o histórico de uma denúncia (antes de excluí-la; o projeto não usa ON DELETE CASCADE). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove o histórico de todas as denúncias de um autor (exclusão de conta e da demonstração). */
    void excluirDoAutor(Long idAutor) throws SQLException;
}
