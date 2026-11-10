package br.com.fiap.meusdireitos.model.enums;

/**
 * Autodeclaração de raça/cor (categorias do IBGE). Resposta opcional e dado sensível (LGPD).
 */
public enum RacaCor implements EnumDescritivo {

    BRANCA("Branca"),
    PRETA("Preta"),
    PARDA("Parda"),
    AMARELA("Amarela"),
    INDIGENA("Indígena"),
    NAO_INFORMADO("Prefiro não informar");

    private final String descricao;

    RacaCor(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    /** @return true para pessoas pretas, pardas ou indígenas (grupo das cotas da Lei 12.711/2012) */
    public boolean isPpi() {
        return this == PRETA || this == PARDA || this == INDIGENA;
    }
}
