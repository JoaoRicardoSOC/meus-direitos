package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.exception.PersistenciaException;
import br.com.fiap.meusdireitos.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executa operações no banco garantindo que a conexão volte ao pool e, quando necessário,
 * que tudo seja confirmado (COMMIT) ou desfeito (ROLLBACK) junto.
 * <p>
 * Uso típico no service:
 * <pre>
 * return Transacao.executar(con -&gt; {
 *     DenunciaDao denunciaDao = new DenunciaDaoJdbc(con);
 *     ...
 *     return resultado;
 * });
 * </pre>
 * Qualquer {@link SQLException} é traduzida por {@link PersistenciaException#de(SQLException)}.
 */
public final class Transacao {

    private static final Logger LOG = Logger.getLogger(Transacao.class.getName());

    private Transacao() {
    }

    /**
     * Executa uma ou mais operações em uma única transação: COMMIT se tudo der certo,
     * ROLLBACK se qualquer exceção ocorrer.
     */
    public static <R> R executar(OperacaoComConexao<R> operacao) {
        Connection conexao = null;
        try {
            conexao = ConnectionFactory.emprestar();
            conexao.setAutoCommit(false);
            try {
                R resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw PersistenciaException.de(e);
        } finally {
            ConnectionFactory.devolver(conexao); // sempre volta ao pool, com ou sem erro
        }
    }

    /** Igual a {@link #executar(OperacaoComConexao)}, para operações sem retorno. */
    public static void executarSemRetorno(AcaoComConexao acao) {
        executar(conexao -> {
            acao.executar(conexao);
            return null;
        });
    }

    /**
     * Executa apenas leituras (SELECT), sem abrir transação explícita.
     */
    public static <R> R consultar(OperacaoComConexao<R> consulta) {
        Connection conexao = null;
        try {
            conexao = ConnectionFactory.emprestar();
            return consulta.executar(conexao);
        } catch (SQLException e) {
            throw PersistenciaException.de(e);
        } finally {
            ConnectionFactory.devolver(conexao);
        }
    }

    private static void desfazer(Connection conexao, Exception causa) {
        try {
            conexao.rollback();
        } catch (SQLException erroRollback) {
            LOG.log(Level.SEVERE, "Falha ao executar ROLLBACK", erroRollback);
            causa.addSuppressed(erroRollback);
        }
    }
}
