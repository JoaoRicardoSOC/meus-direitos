package br.com.fiap.meusdireitos.factory;

import br.com.fiap.meusdireitos.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexões com o Oracle (mesmo padrão do Cap17).
 * <p>
 * Diferenças em relação ao exemplo do curso:
 * <ul>
 *     <li>URL, usuário e senha vêm do {@link AppConfig} (nada de credencial no código);</li>
 *     <li>cada chamada devolve uma conexão nova, que quem chamou fecha com try-with-resources.</li>
 * </ul>
 */
public final class ConnectionFactory {

    /** Tempo máximo (segundos) para abrir a conexão, para a API não travar se o banco estiver fora. */
    private static final int TIMEOUT_CONEXAO_SEGUNDOS = 10;

    /** SQLState da classe 08 (erro de conexão): vira HTTP 503 em PersistenciaException. */
    private static final String SQLSTATE_SEM_CONEXAO = "08001";

    static {
        DriverManager.setLoginTimeout(TIMEOUT_CONEXAO_SEGUNDOS);
    }

    private ConnectionFactory() {
    }

    /**
     * Abre uma nova conexão com o banco. Feche-a sempre (try-with-resources).
     *
     * @return conexão aberta
     * @throws SQLException se as credenciais não estiverem configuradas ou o banco não responder
     */
    public static Connection getConnection() throws SQLException {
        if (!AppConfig.pendencias().isEmpty()) {
            throw new SQLException("Credenciais do banco não configuradas: " + AppConfig.pendencias(),
                    SQLSTATE_SEM_CONEXAO);
        }
        return DriverManager.getConnection(AppConfig.dbUrl(), AppConfig.dbUsuario(), AppConfig.dbSenha());
    }
}
