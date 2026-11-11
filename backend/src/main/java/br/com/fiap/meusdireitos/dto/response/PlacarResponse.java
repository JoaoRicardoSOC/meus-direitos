package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Placar;

/**
 * Placar exibido no card (↑ e ↓) e devolvido após votar, para atualizar a tela na hora.
 *
 * @param meuVoto "UP", "DOWN" ou null (não votou / não está logado)
 */
public record PlacarResponse(int upvotes, int downvotes, int saldo, String meuVoto) {

    public static PlacarResponse de(Placar placar) {
        return new PlacarResponse(placar.upvotes(), placar.downvotes(), placar.saldo(),
                placar.meuVoto() == null ? null : placar.meuVoto().name());
    }
}
