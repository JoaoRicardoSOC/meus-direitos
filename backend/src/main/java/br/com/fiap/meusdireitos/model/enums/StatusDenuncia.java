package br.com.fiap.meusdireitos.model.enums;

/**
 * Ciclo de vida de uma denúncia: ABERTA → EM_ANDAMENTO → RESOLVIDA.
 * Uma denúncia resolvida pode ser reaberta se o problema voltar.
 */
public enum StatusDenuncia implements EnumDescritivo {

    ABERTA("Em aberto"),
    EM_ANDAMENTO("Em andamento"),
    RESOLVIDA("Resolvida");

    private final String descricao;

    StatusDenuncia(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    /**
     * @param novo status desejado
     * @return true se a mudança deste status para {@code novo} é permitida
     */
    public boolean podeIrPara(StatusDenuncia novo) {
        if (novo == null || novo == this) {
            return false;
        }
        switch (this) {
            case ABERTA:
                return novo == EM_ANDAMENTO || novo == RESOLVIDA;
            case EM_ANDAMENTO:
                return true; // pode resolver ou voltar para aberta
            case RESOLVIDA:
                return novo == ABERTA; // reabrir
            default:
                return false;
        }
    }
}
