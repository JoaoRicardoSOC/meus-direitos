package br.com.fiap.meusdireitos.model.enums;

/**
 * Ordenações do feed da cidade (abas da tela "Nossa cidade").
 */
public enum OrdemFeed implements EnumDescritivo {

    /** Prioriza o que mais afeta o perfil do usuário, somado aos votos. */
    PARA_VOCE("Para você"),
    RECENTES("Recentes"),
    MAIS_VOTADOS("Mais votados");

    private final String descricao;

    OrdemFeed(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
