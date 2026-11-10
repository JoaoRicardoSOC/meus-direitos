package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.exception.PersistenciaException;
import br.com.fiap.meusdireitos.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executa operações no banco garantindo que a conexão seja fechada e, quando necessário,
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
        try (Connection conexao = ConnectionFactory.getConnection()) {
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
        try (Connection conexao = ConnectionFactory.getConnection()) {
            return consulta.executar(conexao);
        } catch (SQLException e) {
            throw PersistenciaException.de(e);
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
