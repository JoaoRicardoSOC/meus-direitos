package br.com.fiap.meusdireitos.factory;

import br.com.fiap.meusdireitos.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Fábrica de conexões com o Oracle (mesmo padrão do Cap17), agora com reaproveitamento de conexões.
 * <p>
 * Diferenças em relação ao exemplo do curso:
 * <ul>
 *     <li>URL, usuário e senha vêm do {@link AppConfig} (nada de credencial no código);</li>
 *     <li>as conexões são reaproveitadas por um pool simples ({@link PoolConexoes}, só com o JDK):
 *     abrir uma conexão com o Oracle da FIAP leva centenas de milissegundos, e cada requisição
 *     abria uma nova. Quem chama continua fechando a conexão com try-with-resources; o
 *     {@code close()} apenas a devolve ao pool.</li>
 * </ul>
 * O tamanho do pool vem da variável de ambiente {@code MD_DB_POOL} (padrão 5, entre 1 e 50).
 */
public final class ConnectionFactory {

    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class.getName());

    /** Tempo máximo (segundos) para abrir a conexão, para a API não travar se o banco estiver fora. */
    private static final int TIMEOUT_CONEXAO_SEGUNDOS = 10;

    /** Tempo máximo (segundos) esperando uma conexão livre quando todas estão em uso. */
    private static final int ESPERA_POR_CONEXAO_SEGUNDOS = 10;

    /** SQLState da classe 08 (erro de conexão): vira HTTP 503 em PersistenciaException. */
    private static final String SQLSTATE_SEM_CONEXAO = PoolConexoes.SQLSTATE_SEM_CONEXAO;

    static final String VARIAVEL_TAMANHO_POOL = "MD_DB_POOL";
    static final int TAMANHO_POOL_PADRAO = 5;
    static final int TAMANHO_POOL_MAXIMO = 50;

    private static final PoolConexoes POOL;

    static {
        DriverManager.setLoginTimeout(TIMEOUT_CONEXAO_SEGUNDOS);
        POOL = new PoolConexoes(tamanhoDoPool(System.getenv(VARIAVEL_TAMANHO_POOL)), ConnectionFactory::abrirFisica,
                ESPERA_POR_CONEXAO_SEGUNDOS * 1000L);
        Runtime.getRuntime().addShutdownHook(new Thread(POOL::fecharLivres, "fecha-pool-conexoes"));
    }

    private ConnectionFactory() {
    }

    /**
     * Empresta uma conexão do pool. Feche-a sempre (try-with-resources) para devolvê-la.
     *
     * @return conexão pronta para uso, em autocommit
     * @throws SQLException se as credenciais não estiverem configuradas, o banco não responder ou todas as
     *                      conexões continuarem ocupadas após a espera (SQLState 08001 → HTTP 503)
     */
    public static Connection getConnection() throws SQLException {
        if (!AppConfig.pendencias().isEmpty()) {
            throw new SQLException("Credenciais do banco não configuradas: " + AppConfig.pendencias(),
                    SQLSTATE_SEM_CONEXAO);
        }
        return POOL.emprestar();
    }

    private static Connection abrirFisica() throws SQLException {
        return DriverManager.getConnection(AppConfig.dbUrl(), AppConfig.dbUsuario(), AppConfig.dbSenha());
    }

    /**
     * Interpreta o valor de {@code MD_DB_POOL}. Valor ausente ou inválido usa o padrão; valores fora
     * da faixa são ajustados para 1..{@value #TAMANHO_POOL_MAXIMO}.
     */
    static int tamanhoDoPool(String valor) {
        if (valor == null || valor.isBlank()) {
            return TAMANHO_POOL_PADRAO;
        }
        try {
            return Math.max(1, Math.min(TAMANHO_POOL_MAXIMO, Integer.parseInt(valor.trim())));
        } catch (NumberFormatException e) {
            LOG.warning(VARIAVEL_TAMANHO_POOL + " inválida (\"" + valor + "\"): usando " + TAMANHO_POOL_PADRAO);
            return TAMANHO_POOL_PADRAO;
        }
    }
}
