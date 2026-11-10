package br.com.fiap.meusdireitos.model.enums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tipos de problema urbano (tela "Qual é a categoria?" do protótipo).
 * <p>
 * Cada categoria sabe quais grupos de pessoas ela afeta mais. Ex.: uma calçada quebrada é
 * uma barreira maior para idosos e pessoas com deficiência. É isso que alimenta o feed "Para você".
 */
public enum CategoriaDenuncia implements EnumDescritivo {

    ASFALTO("Asfalto"),
    ILUMINACAO("Iluminação", TagDemografica.IDOSO, TagDemografica.PCD),
    CALCADA_ACESSIBILIDADE("Calçada e acessibilidade",
            TagDemografica.IDOSO, TagDemografica.PCD, TagDemografica.PCD_MOBILIDADE),
    SEGURANCA("Segurança no bairro", TagDemografica.IDOSO),
    LIMPEZA_URBANA("Limpeza urbana"),
    OUTROS("Outros");

    private final String descricao;
    private final Set<TagDemografica> tagsAfetadas;

    CategoriaDenuncia(String descricao, TagDemografica... tagsAfetadas) {
        this.descricao = descricao;
        Set<TagDemografica> tags = EnumSet.noneOf(TagDemografica.class);
        Collections.addAll(tags, tagsAfetadas);
        this.tagsAfetadas = Collections.unmodifiableSet(tags);
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    public Set<TagDemografica> getTagsAfetadas() {
        return tagsAfetadas;
    }

    /**
     * @param tagsUsuario tags calculadas do perfil do usuário
     * @return true se esta categoria afeta especialmente alguém com essas tags
     */
    public boolean afeta(Set<TagDemografica> tagsUsuario) {
        for (TagDemografica tag : tagsUsuario) {
            if (tagsAfetadas.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param tagsUsuario tags calculadas do perfil do usuário
     * @return categorias que devem ganhar prioridade no feed desse usuário (pode ser vazia)
     */
    public static List<CategoriaDenuncia> relevantesPara(Set<TagDemografica> tagsUsuario) {
        List<CategoriaDenuncia> relevantes = new ArrayList<>();
        for (CategoriaDenuncia categoria : values()) {
            if (categoria.afeta(tagsUsuario)) {
                relevantes.add(categoria);
            }
        }
        return relevantes;
    }
}
