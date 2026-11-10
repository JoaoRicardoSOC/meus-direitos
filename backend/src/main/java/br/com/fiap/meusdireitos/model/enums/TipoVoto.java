package br.com.fiap.meusdireitos.model.enums;

/**
 * Voto em uma denúncia: UP confirma o problema, DOWN discorda.
 */
public enum TipoVoto implements EnumDescritivo {

    UP("Apoiar", 1),
    DOWN("Discordar", -1);

    private final String descricao;
    private final int peso;

    TipoVoto(String descricao, int peso) {
        this.descricao = descricao;
        this.peso = peso;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    /** @return quanto o voto soma no saldo da denúncia (+1 ou -1) */
    public int getPeso() {
        return peso;
    }
}
