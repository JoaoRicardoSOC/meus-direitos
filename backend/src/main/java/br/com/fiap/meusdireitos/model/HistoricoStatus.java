package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.time.LocalDateTime;

/**
 * Uma mudança de situação de uma denúncia (TB_GS26S2_HISTORICO_STATUS).
 *
 * @param id         código da mudança (null antes de gravar)
 * @param idDenuncia denúncia que mudou
 * @param anterior   situação antes da mudança
 * @param nova       situação depois da mudança
 * @param origem     quem mudou: o autor ou a comunidade
 * @param data       quando mudou
 */
public record HistoricoStatus(Long id, Long idDenuncia, StatusDenuncia anterior, StatusDenuncia nova,
                              OrigemStatus origem, LocalDateTime data) {
}
