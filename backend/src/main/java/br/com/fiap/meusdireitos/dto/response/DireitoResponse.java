package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Direito;
import br.com.fiap.meusdireitos.model.PassoDireito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.util.ArrayList;
import java.util.List;

/**
 * Tema completo (telas de detalhe): textos, passos "Por onde começar", aviso, links e tags.
 */
public record DireitoResponse(Long id, String titulo, String resumo, String descricao, List<PassoResponse> passos,
                              String aviso, String baseLegal, String linkOficial, String icone, int ordem,
                              List<OpcaoResponse> tags) {

    /** Passo numerado do "Por onde começar". */
    public record PassoResponse(int ordem, String titulo, String texto) {
    }

    public static DireitoResponse de(Direito direito) {
        List<PassoResponse> passos = new ArrayList<>();
        for (PassoDireito passo : direito.getPassos()) {
            passos.add(new PassoResponse(passo.ordem(), passo.titulo(), passo.texto()));
        }
        List<OpcaoResponse> tags = new ArrayList<>();
        for (TagDemografica tag : direito.getTags()) {
            tags.add(OpcaoResponse.de(tag));
        }
        return new DireitoResponse(direito.getId(), direito.getTitulo(), direito.getResumo(), direito.getDescricao(),
                passos, direito.getAviso(), direito.getBaseLegal(), direito.getLinkOficial(),
                direito.getIcone().name(), direito.getOrdem(), tags);
    }
}
