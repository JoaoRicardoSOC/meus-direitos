package br.com.fiap.meusdireitos.model.enums;

/**
 * Ícone exibido no card de cada tema de direitos (o front-end mapeia para a imagem).
 */
public enum IconeDireito implements EnumDescritivo {

    TRANSPORTE("Transporte e mobilidade"),
    PRIORIDADE("Atendimento prioritário"),
    MUNICIPIO("Benefícios municipais"),
    EDUCACAO("Educação"),
    SAUDE("Saúde"),
    RENDA("Renda e assistência"),
    OUTRO("Outro");

    private final String descricao;

    IconeDireito(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
