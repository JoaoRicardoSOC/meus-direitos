package br.com.fiap.meusdireitos.model.enums;

/**
 * Onde a pessoa concluiu os estudos (usado para cotas de escola pública).
 */
public enum PercursoEscolar implements EnumDescritivo {

    ESCOLA_PUBLICA("Escola pública"),
    ESCOLA_PARTICULAR("Escola particular"),
    MISTO("Percurso misto"),
    NAO_INFORMADO("Prefiro não informar");

    private final String descricao;

    PercursoEscolar(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
