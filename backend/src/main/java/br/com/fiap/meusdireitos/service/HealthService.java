package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dto.response.HealthResponse;
import br.com.fiap.meusdireitos.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Verifica se a API e o banco de dados estão respondendo.
 */
public class HealthService {

    private static final Logger LOG = Logger.getLogger(HealthService.class.getName());

    public static final String UP = "UP";
    public static final String INDISPONIVEL = "INDISPONIVEL";

    private static final int TIMEOUT_VALIDACAO_SEGUNDOS = 5;

    /**
     * @return estado da API e do banco. Nunca lança exceção: se o banco estiver fora,
     * o campo {@code banco} vem como "INDISPONIVEL" e a API continua respondendo.
     */
    public HealthResponse verificar() {
        return new HealthResponse(UP, estadoDoBanco(), LocalDateTime.now());
    }

    private String estadoDoBanco() {
        try (Connection conexao = ConnectionFactory.getConnection()) {
            return conexao.isValid(TIMEOUT_VALIDACAO_SEGUNDOS) ? UP : INDISPONIVEL;
        } catch (SQLException e) {
            LOG.warning(() -> "Banco indisponível no health check: " + e.getMessage());
            return INDISPONIVEL;
        }
    }
}
