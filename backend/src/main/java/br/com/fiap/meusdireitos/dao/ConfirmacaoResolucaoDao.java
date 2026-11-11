package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.ConfirmacaoResolucao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Confirmações de moradores de que o problema foi resolvido (TB_CONFIRMACAO_RESOLUCAO,
 * tabela associativa N:N entre usuário e denúncia).
 */
public interface ConfirmacaoResolucaoDao {

    /** @return true se a confirmação foi registrada agora; false se a pessoa já tinha confirmado */
    boolean confirmar(Long idUsuario, Long idDenuncia) throws SQLException;

    /** Igual a {@link #confirmar(Long, Long)}, com a data informada (conteúdo de demonstração). */
    boolean confirmar(Long idUsuario, Long idDenuncia, LocalDateTime quando) throws SQLException;

    /** Grava várias confirmações de uma vez (lote JDBC), para o conteúdo de demonstração. */
    void confirmarEmLote(List<ConfirmacaoResolucao> confirmacoes) throws SQLException;

    /** @return true se havia confirmação e ela foi retirada */
    boolean retirar(Long idUsuario, Long idDenuncia) throws SQLException;

    int contar(Long idDenuncia) throws SQLException;

    /** Remove todas as confirmações da denúncia (ao reabrir ou excluir). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove as confirmações feitas pelo usuário e as recebidas nas denúncias dele (exclusão de conta). */
    void excluirDoUsuario(Long idUsuario) throws SQLException;
}
