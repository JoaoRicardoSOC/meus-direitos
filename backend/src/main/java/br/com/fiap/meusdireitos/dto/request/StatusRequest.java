package br.com.fiap.meusdireitos.dto.request;

import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

/**
 * Novo status da denúncia (ex.: RESOLVIDA).
 */
public record StatusRequest(StatusDenuncia status) {
}
