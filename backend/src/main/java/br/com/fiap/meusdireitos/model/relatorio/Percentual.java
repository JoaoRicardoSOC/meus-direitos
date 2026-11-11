package br.com.fiap.meusdireitos.model.relatorio;

/**
 * Cálculo de percentual usado nos relatórios.
 */
public final class Percentual {

    private Percentual() {
    }

    /**
     * @return parte/total em %, com uma casa decimal (0 quando o total é zero)
     */
    public static double de(int parte, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(parte * 1000.0 / total) / 10.0;
    }
}
