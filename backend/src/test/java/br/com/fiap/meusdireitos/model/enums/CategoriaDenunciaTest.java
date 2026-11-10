package br.com.fiap.meusdireitos.model.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaDenunciaTest {

    @Test
    void calcadaAfetaPessoaComDeficienciaFisica() {
        assertTrue(CategoriaDenuncia.CALCADA_ACESSIBILIDADE.afeta(EnumSet.of(TagDemografica.PCD_MOBILIDADE)));
    }

    @Test
    void asfaltoNaoTemPublicoPrioritario() {
        assertFalse(CategoriaDenuncia.ASFALTO.afeta(EnumSet.allOf(TagDemografica.class)));
    }

    @Test
    void personaSrAntonioPriorizaCalcadaIluminacaoESeguranca() {
        Set<TagDemografica> antonio = EnumSet.of(TagDemografica.IDOSO, TagDemografica.PCD, TagDemografica.PCD_MOBILIDADE);

        List<CategoriaDenuncia> relevantes = CategoriaDenuncia.relevantesPara(antonio);

        assertEquals(List.of(CategoriaDenuncia.ILUMINACAO, CategoriaDenuncia.CALCADA_ACESSIBILIDADE,
                CategoriaDenuncia.SEGURANCA), relevantes);
    }

    @Test
    void usuarioSemTagsNaoTemCategoriaPrioritaria() {
        assertTrue(CategoriaDenuncia.relevantesPara(EnumSet.noneOf(TagDemografica.class)).isEmpty());
    }
}
