package br.com.fiap.meusdireitos.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pool simples de conexões, feito só com o JDK (sem biblioteca nova).
 * <p>
 * Como funciona:
 * <ul>
 *     <li>no máximo {@code tamanho} conexões físicas existem ao mesmo tempo; um {@link Semaphore}
 *     controla quantas estão emprestadas;</li>
 *     <li>as conexões livres ficam numa {@link BlockingQueue}. Ao emprestar, a conexão livre é testada
 *     com {@code isValid}; se o banco a derrubou, ela é descartada e outra é aberta;</li>
 *     <li>quem pede recebe um <b>proxy</b> da conexão. O {@code close()} do proxy não fecha a conexão
 *     física: desfaz o que ficou pendente (ROLLBACK), volta ao autocommit e devolve a conexão ao pool.
 *     Por isso o resto do código continua usando try-with-resources normalmente;</li>
 *     <li>se todas estiverem em uso, espera até {@code esperaMaxima}; depois disso lança
 *     {@link SQLException} com SQLState 08001 (a API responde 503).</li>
 * </ul>
 */
final class PoolConexoes {

    private static final Logger LOG = Logger.getLogger(PoolConexoes.class.getName());

    /** Segundos dados ao {@code isValid} para responder antes de a conexão ser considerada morta. */
    static final int TIMEOUT_VALIDACAO_SEGUNDOS = 2;
    /** SQLState da classe 08 (erro de conexão): vira HTTP 503 em PersistenciaException. */
    static final String SQLSTATE_SEM_CONEXAO = "08001";
    /** SQLState de "conexão inexistente": usar o proxy depois de fechá-lo. */
    static final String SQLSTATE_CONEXAO_FECHADA = "08003";

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
     * Empresta uma conexão. Feche-a (try-with-resources) para devolvê-la ao pool.
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
            return embrulhar(obterFisica());
        } catch (SQLException | RuntimeException e) {
            vagas.release(); // não conseguiu abrir: a vaga volta a ficar disponível
            throw e;
        }
    }

    /** Conexão livre e válida do pool, ou uma nova se não houver nenhuma. */
    private Connection obterFisica() throws SQLException {
        Connection fisica;
        while ((fisica = livres.poll()) != null) {
            if (valida(fisica)) {
                return fisica;
            }
            fecharFisica(fisica); // derrubada pelo banco (timeout, reinício...): descarta e tenta a próxima
        }
        return abridor.abrir();
    }

    /**
     * Prepara a conexão para o próximo uso e a guarda na fila. Se não for possível limpá-la,
     * ou a fila já estiver cheia, a conexão física é fechada.
     */
    private void devolver(Connection fisica) {
        try {
            if (limpar(fisica) && livres.offer(fisica)) {
                return;
            }
            fecharFisica(fisica);
        } finally {
            vagas.release();
        }
    }

    private static boolean limpar(Connection fisica) {
        try {
            if (fisica.isClosed()) {
                return false;
            }
            if (!fisica.getAutoCommit()) {
                fisica.rollback(); // nada que ficou sem COMMIT passa para o próximo usuário
                fisica.setAutoCommit(true);
            }
            if (fisica.isReadOnly()) {
                fisica.setReadOnly(false);
            }
            return true;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Conexão descartada ao voltar para o pool: " + e.getMessage());
            return false;
        }
    }

    private static boolean valida(Connection fisica) {
        try {
            return fisica.isValid(TIMEOUT_VALIDACAO_SEGUNDOS);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void fecharFisica(Connection fisica) {
        try {
            fisica.close();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Falha ao fechar conexão descartada", e);
        }
    }

    /** Fecha as conexões livres (ao encerrar a aplicação). As emprestadas fecham quando forem devolvidas. */
    void fecharLivres() {
        Connection fisica;
        while ((fisica = livres.poll()) != null) {
            fecharFisica(fisica);
        }
    }

    /** @return conexões físicas paradas na fila (para testes e diagnóstico) */
    int quantidadeLivres() {
        return livres.size();
    }

    /** @return vagas para novos empréstimos (para testes e diagnóstico) */
    int vagasDisponiveis() {
        return vagas.availablePermits();
    }

    private Connection embrulhar(Connection fisica) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, new ConexaoEmprestada(fisica));
    }

    /** Comportamento do proxy entregue a quem pediu a conexão. */
    private final class ConexaoEmprestada implements InvocationHandler {

        private final Connection fisica;
        private final AtomicBoolean devolvida = new AtomicBoolean(false);

        ConexaoEmprestada(Connection fisica) {
            this.fisica = fisica;
        }

        @Override
        public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
            switch (metodo.getName()) {
                case "close":
                    if (devolvida.compareAndSet(false, true)) { // fechar duas vezes devolve uma vez só
                        devolver(fisica);
                    }
                    return null;
                case "isClosed":
                    return devolvida.get() || fisica.isClosed();
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "ConexaoDoPool[" + fisica + (devolvida.get() ? ", devolvida]" : "]");
                default:
                    break;
            }
            if (devolvida.get()) {
                throw new SQLException("A conexão já foi devolvida ao pool.", SQLSTATE_CONEXAO_FECHADA);
            }
            try {
                return metodo.invoke(fisica, args);
            } catch (InvocationTargetException e) {
                throw e.getCause(); // repassa a SQLException original do driver
            }
        }
    }
}
