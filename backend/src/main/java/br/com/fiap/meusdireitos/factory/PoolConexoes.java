package br.com.fiap.meusdireitos.factory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pool simples de conexões, feito só com o JDK (sem biblioteca nova).
 * <p>
 * Por que existe: abrir uma conexão com o Oracle da FIAP leva ~300 ms, e uma consulta leva ~30 ms.
 * Sem reaproveitar conexões, cada tela do app ficaria várias vezes mais lenta.
 * <p>
 * Como funciona:
 * <ul>
 *     <li>{@link #emprestar()} entrega uma conexão parada na fila {@code livres} (testada com
 *     {@code isValid}) ou, se não houver, abre uma nova;</li>
 *     <li>{@link #devolver(Connection)} desfaz o que ficou sem COMMIT, volta ao autocommit e guarda a
 *     conexão na fila para a próxima requisição;</li>
 *     <li>o {@link Semaphore} {@code vagas} limita quantas conexões existem ao mesmo tempo. Se todas
 *     estiverem emprestadas, quem pede espera até {@code esperaMaxima}; depois disso recebe
 *     {@link SQLException} com SQLState 08001 (a API responde 503).</li>
 * </ul>
 * Quem empresta precisa devolver. No sistema, só a {@code Transacao} e o health check fazem isso,
 * sempre num bloco {@code finally}.
 */
final class PoolConexoes {

    private static final Logger LOG = Logger.getLogger(PoolConexoes.class.getName());

    /** Segundos dados ao {@code isValid} para responder antes de a conexão ser considerada morta. */
    static final int TIMEOUT_VALIDACAO_SEGUNDOS = 2;
    /** SQLState da classe 08 (erro de conexão): vira HTTP 503 em PersistenciaException. */
    static final String SQLSTATE_SEM_CONEXAO = "08001";

    /** Abre uma conexão física nova (no uso real, {@code DriverManager.getConnection}). */
    @FunctionalInterface
    interface AbridorConexao {
        Connection abrir() throws SQLException;
    }

    private final int tamanho;
    private final AbridorConexao abridor;
    private final long esperaMaximaMs;
    private final BlockingQueue<Connection> livres;
    private final Semaphore vagas;
    /** Conexões emprestadas agora: devolver a mesma duas vezes (ou uma que não é do pool) não faz nada. */
    private final Set<Connection> emprestadas = ConcurrentHashMap.newKeySet();

    PoolConexoes(int tamanho, AbridorConexao abridor, long esperaMaximaMs) {
        if (tamanho < 1) {
            throw new IllegalArgumentException("O pool precisa de pelo menos uma conexão");
        }
        this.tamanho = tamanho;
        this.abridor = abridor;
        this.esperaMaximaMs = esperaMaximaMs;
        this.livres = new LinkedBlockingQueue<>(tamanho);
        this.vagas = new Semaphore(tamanho, true);
    }

    /**
     * Empresta uma conexão, em autocommit. Devolva-a com {@link #devolver(Connection)}.
     *
     * @throws SQLException SQLState 08001 se nenhuma vaga abrir dentro do tempo de espera, ou o erro do driver
     */
    Connection emprestar() throws SQLException {
        try {
            if (!vagas.tryAcquire(esperaMaximaMs, TimeUnit.MILLISECONDS)) {
                throw new SQLException("Todas as " + tamanho + " conexões com o banco estão em uso. "
                        + "Tente novamente em instantes.", SQLSTATE_SEM_CONEXAO);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Espera por conexão interrompida.", SQLSTATE_SEM_CONEXAO, e);
        }
        try {
            Connection conexao = livreOuNova();
            emprestadas.add(conexao);
            return conexao;
        } catch (SQLException | RuntimeException e) {
            vagas.release(); // não conseguiu abrir: a vaga volta a ficar disponível
            throw e;
        }
    }

    /**
     * Devolve uma conexão emprestada. Se não for possível limpá-la, ou a fila já estiver cheia,
     * ela é fechada. Aceita {@code null} e conexões já devolvidas (não faz nada).
     */
    void devolver(Connection conexao) {
        if (conexao == null || !emprestadas.remove(conexao)) {
            return;
        }
        try {
            if (!limpar(conexao) || !livres.offer(conexao)) {
                fechar(conexao);
            }
        } finally {
            vagas.release();
        }
    }

    /** Conexão livre e válida da fila, ou uma nova se não houver nenhuma. */
    private Connection livreOuNova() throws SQLException {
        Connection conexao;
        while ((conexao = livres.poll()) != null) {
            if (valida(conexao)) {
                return conexao;
            }
            fechar(conexao); // derrubada pelo banco (tempo parado, reinício...): descarta e tenta a próxima
        }
        return abridor.abrir();
    }

    /** Deixa a conexão como nova: nada que ficou sem COMMIT passa para o próximo usuário. */
    private static boolean limpar(Connection conexao) {
        try {
            if (conexao.isClosed()) {
                return false;
            }
            if (!conexao.getAutoCommit()) {
                conexao.rollback();
                conexao.setAutoCommit(true);
            }
            if (conexao.isReadOnly()) {
                conexao.setReadOnly(false);
            }
            return true;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Conexão descartada ao voltar para o pool: " + e.getMessage());
            return false;
        }
    }

    private static boolean valida(Connection conexao) {
        try {
            return conexao.isValid(TIMEOUT_VALIDACAO_SEGUNDOS);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void fechar(Connection conexao) {
        try {
            conexao.close();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Falha ao fechar conexão descartada", e);
        }
    }

    /** Fecha as conexões livres (ao encerrar a aplicação). */
    void fecharLivres() {
        Connection conexao;
        while ((conexao = livres.poll()) != null) {
            fechar(conexao);
        }
    }

    /** @return conexões paradas na fila (para testes e diagnóstico) */
    int quantidadeLivres() {
        return livres.size();
    }

    /** @return vagas para novos empréstimos (para testes e diagnóstico) */
    int vagasDisponiveis() {
        return vagas.availablePermits();
    }
}
