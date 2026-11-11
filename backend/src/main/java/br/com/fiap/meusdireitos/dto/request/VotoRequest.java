package br.com.fiap.meusdireitos.dto.request;

import br.com.fiap.meusdireitos.model.enums.TipoVoto;

/**
 * Voto em uma denúncia: UP (apoiar) ou DOWN (discordar).
 */
public record VotoRequest(TipoVoto tipo) {
}
