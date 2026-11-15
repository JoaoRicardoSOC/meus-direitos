package br.com.fiap.meusdireitos.model.enums;

/**
 * Quem mudou a situação de uma denúncia (linha do tempo, TB_GS26S2_HISTORICO_STATUS).
 */
public enum OrigemStatus implements EnumDescritivo {

    /** O próprio autor marcou "em andamento", "resolvida" ou reabriu. */
    AUTOR("Pelo autor"),
    /** Três moradores da cidade confirmaram que o problema foi resolvido. */
    COMUNIDADE("Pela comunidade");

    private final String descricao;

    OrigemStatus(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
