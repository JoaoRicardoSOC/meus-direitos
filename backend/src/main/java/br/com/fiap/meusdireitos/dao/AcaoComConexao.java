package br.com.fiap.meusdireitos.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Trecho de código que usa uma conexão e não devolve resultado (ex.: excluir).
 * Interface funcional: pode ser escrita como lambda.
 */
@FunctionalInterface
public interface AcaoComConexao {

    void executar(Connection conexao) throws SQLException;
}
