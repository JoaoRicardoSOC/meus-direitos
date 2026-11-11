package br.com.fiap.meusdireitos.model.relatorio;

/**
 * "Números da cidade": indicadores de participação e de impacto (ODS 11).
 */
public record ResumoCidade(int usuarios, int denuncias, int abertas, int emAndamento, int resolvidas,
                           int votos, int comentarios, int confirmacoes, int temasDeDireitos,
                           int denunciasSimuladas) {

    /** @return percentual de denúncias resolvidas na cidade (indicador de impacto) */
    public double percentualResolvidas() {
        return Percentual.de(resolvidas, denuncias);
    }
}
