package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.EventoLinhaDoTempo;

import java.time.LocalDateTime;

/**
 * Um passo da linha do tempo da denúncia (GET /denuncias/{id}/historico).
 *
 * @param situacao situação a partir deste passo
 * @param origem   "Pelo autor" ou "Pela comunidade"; omitido no registro e em mudanças anteriores ao histórico
 * @param registro true só no primeiro passo (a denúncia foi registrada)
 * @param em       momento do passo
 */
public record EventoHistoricoResponse(OpcaoResponse situacao, OpcaoResponse origem, boolean registro,
                                      LocalDateTime em) {

    public static EventoHistoricoResponse de(EventoLinhaDoTempo evento) {
        return new EventoHistoricoResponse(OpcaoResponse.de(evento.situacao()), OpcaoResponse.de(evento.origem()),
                evento.registro(), evento.quando());
    }
}
