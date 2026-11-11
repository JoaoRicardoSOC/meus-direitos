package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;

import java.sql.SQLException;
import java.util.List;

/**
 * Operações básicas de persistência (CRUD) comuns às entidades.
 * Cada DAO específica estende esta interface e acrescenta suas consultas.
 *
 * @param <T> tipo da entidade
 */
public interface CrudDao<T> {

    /**
     * Grava uma nova entidade.
     *
     * @return a mesma entidade, já com o id gerado pelo banco
     */
    T inserir(T entidade) throws SQLException;

    /**
     * @throws EntidadeNaoEncontradaException se não existir registro com o id
     */
    T buscarPorId(Long id) throws SQLException;

    List<T> listar() throws SQLException;

    /**
     * @throws EntidadeNaoEncontradaException se nenhuma linha foi atualizada
     */
    void atualizar(T entidade) throws SQLException;

    /**
     * @throws EntidadeNaoEncontradaException se nenhuma linha foi excluída
     */
    void excluir(Long id) throws SQLException;
}
