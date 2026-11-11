package br.com.fiap.meusdireitos.model.relatorio;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;

/**
 * Quantidade de denúncias de uma categoria, por situação.
 */
public record TotalPorCategoria(CategoriaDenuncia categoria, int total, int abertas, int emAndamento,
                                int resolvidas) {

    public double percentualResolvidas() {
        return Percentual.de(resolvidas, total);
    }
}
