package br.com.fiap.meusdireitos.model;

import java.time.LocalDateTime;

/**
 * Confirmação de um morador de que o problema da denúncia foi resolvido
 * (tabela associativa TB_GS26S2_CONFIRMACAO_RESOLUCAO).
 */
public record ConfirmacaoResolucao(Long idUsuario, Long idDenuncia, LocalDateTime data) {
}
