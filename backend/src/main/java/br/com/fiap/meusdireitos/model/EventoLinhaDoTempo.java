package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Um passo da linha do tempo de uma denúncia, como o app mostra.
 *
 * @param situacao situação a partir deste momento
 * @param origem   quem mudou; null no registro e quando a mudança é anterior ao histórico
 * @param registro true só no primeiro passo (a denúncia foi registrada)
 * @param quando   momento do passo
 */
public record EventoLinhaDoTempo(StatusDenuncia situacao, OrigemStatus origem, boolean registro,
                                 LocalDateTime quando) {

    /**
     * Monta a linha do tempo: o registro (data de criação) e depois cada mudança gravada, em ordem.
     * <p>
     * Denúncias anteriores ao histórico (ou de exemplo antigas) podem não ter mudanças gravadas mesmo
     * estando em andamento ou resolvidas; nesse caso entra um passo com a situação atual na data da
     * última atualização, sem origem, para a linha do tempo não contradizer a situação mostrada.
     */
    public static List<EventoLinhaDoTempo> montar(Denuncia denuncia, List<HistoricoStatus> mudancas) {
        List<EventoLinhaDoTempo> passos = new ArrayList<>();
        passos.add(new EventoLinhaDoTempo(StatusDenuncia.ABERTA, null, true, denuncia.getDataCriacao()));
        List<HistoricoStatus> emOrdem = new ArrayList<>(mudancas);
        emOrdem.sort(Comparator.comparing(HistoricoStatus::data, Comparator.nullsLast(Comparator.naturalOrder())));
        for (HistoricoStatus mudanca : emOrdem) {
            passos.add(new EventoLinhaDoTempo(mudanca.nova(), mudanca.origem(), false, mudanca.data()));
        }
        if (emOrdem.isEmpty() && denuncia.getStatus() != null && denuncia.getStatus() != StatusDenuncia.ABERTA) {
            passos.add(new EventoLinhaDoTempo(denuncia.getStatus(), null, false, denuncia.getDataAtualizacao()));
        }
        return passos;
    }
}
