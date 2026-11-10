package br.com.fiap.meusdireitos.model.enums;

/**
 * Características do cidadão, calculadas a partir do perfil demográfico.
 * Ligam o perfil aos direitos (temas) e às categorias de denúncia que mais o afetam.
 */
public enum TagDemografica implements EnumDescritivo {

    IDOSO("Pessoa idosa (60+)"),
    JOVEM("Jovem (15 a 29 anos)"),
    PCD("Pessoa com deficiência"),
    PCD_MOBILIDADE("Deficiência física / mobilidade"),
    PPI("Pessoa preta, parda ou indígena"),
    ESCOLA_PUBLICA("Estudou em escola pública"),
    BAIXA_RENDA("Família no CadÚnico"),
    /** Usada apenas no catálogo: tema que vale para qualquer pessoa. */
    TODOS("Todas as pessoas");

    private final String descricao;

    TagDemografica(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
