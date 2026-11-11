package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Direito;

/**
 * Card de um tema na tela "Meus Direitos" (sem os passos).
 *
 * @param icone código do ícone (ex.: TRANSPORTE), mapeado para imagem pelo front-end
 */
public record DireitoResumoResponse(Long id, String titulo, String resumo, String icone) {

    public static DireitoResumoResponse de(Direito direito) {
        return new DireitoResumoResponse(direito.getId(), direito.getTitulo(), direito.getResumo(),
                direito.getIcone().name());
    }
}
