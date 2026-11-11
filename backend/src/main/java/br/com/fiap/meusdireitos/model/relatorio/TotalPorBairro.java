package br.com.fiap.meusdireitos.model.relatorio;

/**
 * Concentração de problemas por bairro (ajuda a priorizar a zeladoria).
 *
 * @param saldoVotos soma dos votos (apoios menos discordâncias) das denúncias do bairro
 */
public record TotalPorBairro(String bairro, int total, int resolvidas, int saldoVotos) {

    public double percentualResolvidas() {
        return Percentual.de(resolvidas, total);
    }
}
