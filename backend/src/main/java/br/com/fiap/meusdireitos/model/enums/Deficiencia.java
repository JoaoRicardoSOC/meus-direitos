package br.com.fiap.meusdireitos.model.enums;

/**
 * Tipo de deficiência informado no questionário. Resposta opcional e dado sensível (LGPD).
 */
public enum Deficiencia implements EnumDescritivo {

    NENHUMA("Nenhuma"),
    FISICA("Física"),
    VISUAL("Visual"),
    AUDITIVA("Auditiva"),
    INTELECTUAL("Intelectual"),
    NAO_INFORMADO("Prefiro não informar");

    private final String descricao;

    Deficiencia(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    /** @return true quando a pessoa informou ter alguma deficiência */
    public boolean isDeficiencia() {
        return this != NENHUMA && this != NAO_INFORMADO;
    }
}
