package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilDemograficoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private static PerfilDemografico nascidoEm(LocalDate data) {
        PerfilDemografico perfil = new PerfilDemografico();
        perfil.setDataNascimento(data);
        return perfil;
    }

    @Test
    void idadeSoCompletaNoDiaDoAniversario() {
        assertEquals(60, nascidoEm(LocalDate.of(1966, 11, 11)).calcularIdade(HOJE));
        assertEquals(59, nascidoEm(LocalDate.of(1966, 11, 12)).calcularIdade(HOJE));
    }

    @Test
    void idosoAPartirDe60Anos() {
        assertTrue(nascidoEm(LocalDate.of(1966, 11, 11)).calcularTags(HOJE).contains(TagDemografica.IDOSO));
        assertFalse(nascidoEm(LocalDate.of(1966, 11, 12)).calcularTags(HOJE).contains(TagDemografica.IDOSO));
    }

    @Test
    void jovemEntre15e29Anos() {
        assertTrue(nascidoEm(LocalDate.of(2011, 11, 11)).calcularTags(HOJE).contains(TagDemografica.JOVEM));  // 15
        assertTrue(nascidoEm(LocalDate.of(1996, 11, 12)).calcularTags(HOJE).contains(TagDemografica.JOVEM));  // 29
        assertFalse(nascidoEm(LocalDate.of(1996, 11, 11)).calcularTags(HOJE).contains(TagDemografica.JOVEM)); // 30
        assertFalse(nascidoEm(LocalDate.of(2011, 11, 12)).calcularTags(HOJE).contains(TagDemografica.JOVEM)); // 14
    }

    @Test
    void personaSrAntonio() {
        PerfilDemografico antonio = nascidoEm(LocalDate.of(1958, 3, 12));
        antonio.setDeficiencia(Deficiencia.FISICA);
        antonio.setPercursoEscolar(PercursoEscolar.ESCOLA_PUBLICA);
        antonio.setBaixaRenda(true);

        assertEquals(EnumSet.of(TagDemografica.IDOSO, TagDemografica.PCD, TagDemografica.PCD_MOBILIDADE,
                TagDemografica.ESCOLA_PUBLICA, TagDemografica.BAIXA_RENDA), antonio.calcularTags(HOJE));
    }

    @Test
    void deficienciaNaoFisicaNaoGeraMobilidade() {
        PerfilDemografico perfil = new PerfilDemografico();
        perfil.setDeficiencia(Deficiencia.VISUAL);

        Set<TagDemografica> tags = perfil.calcularTags(HOJE);
        assertTrue(tags.contains(TagDemografica.PCD));
        assertFalse(tags.contains(TagDemografica.PCD_MOBILIDADE));
    }

    @Test
    void racaCorPpiGeraTag() {
        PerfilDemografico perfil = new PerfilDemografico();
        perfil.setRacaCor(RacaCor.PARDA);
        assertEquals(EnumSet.of(TagDemografica.PPI), perfil.calcularTags(HOJE));
    }

    @Test
    void respostasNaoInformadasNaoGeramTags() {
        PerfilDemografico perfil = new PerfilDemografico();
        perfil.setRacaCor(RacaCor.NAO_INFORMADO);
        perfil.setDeficiencia(Deficiencia.NAO_INFORMADO);
        perfil.setPercursoEscolar(PercursoEscolar.NAO_INFORMADO);
        perfil.setBaixaRenda(false);

        assertTrue(perfil.calcularTags(HOJE).isEmpty());
        assertTrue(new PerfilDemografico().calcularTags(HOJE).isEmpty());
        assertNull(new PerfilDemografico().calcularIdade(HOJE));
    }
}
