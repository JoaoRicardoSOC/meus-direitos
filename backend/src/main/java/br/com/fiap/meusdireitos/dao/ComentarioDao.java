package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Comentario;

import java.sql.SQLException;
import java.util.List;

/**
 * Acesso aos comentários da "Conversa do bairro" (TB_COMENTARIO).
 */
public interface ComentarioDao extends CrudDao<Comentario> {

    /**
     * Busca um comentário que ainda aparece na conversa (não foi ocultado por reportes).
     *
     * @throws br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException se não existir ou estiver oculto
     */
    Comentario buscarVisivelPorId(Long id) throws SQLException;

    /** @return comentários da denúncia, do mais antigo para o mais novo */
    List<Comentario> listarPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove todos os comentários de uma denúncia (usado ao excluí-la). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /** Grava vários comentários de uma vez (lote JDBC), para o conteúdo de demonstração. */
    void inserirEmLote(List<Comentario> comentarios) throws SQLException;

    /** Remove os comentários escritos pelo usuário e os feitos nas denúncias dele (exclusão de conta). */
    void excluirDoUsuario(Long idUsuario) throws SQLException;
}
