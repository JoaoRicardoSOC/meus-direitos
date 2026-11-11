package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.TipoVoto;

/**
 * Contagem de votos de uma denúncia, calculada no banco (COUNT/CASE).
 *
 * @param upvotes   votos "apoiar"
 * @param downvotes votos "discordar"
 * @param meuVoto   voto de quem está consultando (null se não votou ou não está logado)
 */
public record Placar(int upvotes, int downvotes, TipoVoto meuVoto) {

    /** @return saldo exibido no card (apoios menos discordâncias) */
    public int saldo() {
        return upvotes * TipoVoto.UP.getPeso() + downvotes * TipoVoto.DOWN.getPeso();
    }
}
