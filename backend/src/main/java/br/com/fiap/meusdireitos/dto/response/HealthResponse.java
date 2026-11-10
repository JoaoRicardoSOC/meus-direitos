package br.com.fiap.meusdireitos.dto.response;

import java.time.LocalDateTime;

/**
 * Estado da API devolvido por GET /api/health.
 *
 * @param status   estado da API ("UP" quando está respondendo)
 * @param banco    estado da conexão com o Oracle ("UP" ou "INDISPONIVEL")
 * @param dataHora momento da verificação
 */
public record HealthResponse(String status, String banco, LocalDateTime dataHora) {
}
