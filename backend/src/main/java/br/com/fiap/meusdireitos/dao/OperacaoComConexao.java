package br.com.fiap.meusdireitos.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Trecho de código que usa uma conexão e devolve um resultado.
 * Interface funcional: pode ser escrita como lambda ({@code con -> dao.buscar(...)}).
 *
 * @param <R> tipo do resultado
 */
@FunctionalInterface
public interface OperacaoComConexao<R> {

    R executar(Connection conexao) throws SQLException;
}
