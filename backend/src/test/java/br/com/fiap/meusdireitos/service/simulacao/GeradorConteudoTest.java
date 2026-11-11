package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ComentarioPlanejado;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ConfirmacaoPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.DenunciaPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.VotoPlanejado;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorConteudoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 12, 15, 0);
    private static CatalogoSimulacao catalogo;

    /** As garantias valem para qualquer semente: testamos várias cidades. */
    private static final List<String> CIDADES = List.of("SAO PAULO", "CAMPINAS", "RIO DE JANEIRO", "RECIFE",
            "MANAUS", "CURITIBA", "PORTO ALEGRE", "SALVADOR", "FORTALEZA", "BELEM", "GOIANIA", "NATAL",
            "SANTOS", "OSASCO", "SOROCABA", "LONDRINA", "JOINVILLE", "UBERLANDIA", "MACEIO", "TERESINA",
            "CAMPO GRANDE", "JOAO PESSOA", "ARACAJU", "PALMAS", "BOA VISTA", "MACAPA", "RIO BRANCO",
            "PORTO VELHO", "CUIABA", "VITORIA");

    @BeforeAll
    static void carregarCatalogo() {
        catalogo = CatalogoSimulacao.carregar();
    }

    private static PacoteCidade pacoteDe(String cidade) {
        return new GeradorConteudo(GeradorConteudo.randomDaCidade(cidade, "SP"), catalogo).gerarPacote(AGORA);
    }

    @Test
    void catalogoTemModelosSuficientesPorCategoria() {
        for (CategoriaDenuncia categoria : CategoriaDenuncia.values()) {
            assertTrue(catalogo.modelosDa(categoria).size() >= 3, "poucos modelos de " + categoria);
        }
        assertTrue(catalogo.moradores().size() >= GeradorConteudo.MORADORES_POR_CIDADE);
    }

    @Test
    void mesmaCidadeGeraSempreOMesmoPacote() {
        PacoteCidade a = pacoteDe("SAO PAULO");
        PacoteCidade b = pacoteDe("SAO PAULO");
        assertEquals(a, b);
        assertNotEquals(a, pacoteDe("CAMPINAS"));
    }

    @Test
    void pacoteTemDozeMoradoresDistintosEDozeDenunciasComTitulosUnicos() {
        for (String cidade : CIDADES) {
            pacoteTemDozeMoradoresDistintosEDozeDenunciasComTitulosUnicos(cidade);
        }
    }

    private void pacoteTemDozeMoradoresDistintosEDozeDenunciasComTitulosUnicos(String cidade) {
        PacoteCidade pacote = pacoteDe(cidade);
        assertEquals(12, new HashSet<>(pacote.moradores()).size());
        assertEquals(12, pacote.denuncias().size());
        Set<String> titulos = new HashSet<>();
        for (DenunciaPlanejada d : pacote.denuncias()) {
            assertTrue(titulos.add(d.titulo()), "título repetido: " + d.titulo());
            assertFalse(d.titulo().contains("{") || d.descricao().contains("{"), "marcador não preenchido");
        }
    }

    @Test
    void ninguemInterageComoOutroNaPropriaDenunciaNemVotaDuasVezes() {
        for (String cidade : CIDADES) {
            ninguemInterageComoOutroNaPropriaDenunciaNemVotaDuasVezes(cidade);
        }
    }

    private void ninguemInterageComoOutroNaPropriaDenunciaNemVotaDuasVezes(String cidade) {
        for (DenunciaPlanejada d : pacoteDe(cidade).denuncias()) {
            Set<Integer> votantes = new HashSet<>();
            for (VotoPlanejado v : d.votos()) {
                assertNotEquals(d.autor(), v.morador(), "autor votou na própria denúncia");
                assertTrue(votantes.add(v.morador()), "voto duplicado");
            }
            Set<Integer> confirmadores = new HashSet<>();
            for (ConfirmacaoPlanejada c : d.confirmacoes()) {
                assertNotEquals(d.autor(), c.morador(), "autor confirmou a própria denúncia");
                assertTrue(confirmadores.add(c.morador()), "confirmação duplicada");
            }
        }
    }

    @Test
    void interacoesAcontecemDepoisDoPostEAntesDeAgora() {
        for (String cidade : CIDADES) {
            interacoesAcontecemDepoisDoPostEAntesDeAgora(cidade);
        }
    }

    private void interacoesAcontecemDepoisDoPostEAntesDeAgora(String cidade) {
        for (DenunciaPlanejada d : pacoteDe(cidade).denuncias()) {
            assertTrue(d.criadaEm().isBefore(AGORA));
            for (VotoPlanejado v : d.votos()) {
                assertTrue(v.quando().isAfter(d.criadaEm()) && !v.quando().isAfter(AGORA));
            }
            for (ComentarioPlanejado c : d.comentarios()) {
                assertTrue(c.quando().isAfter(d.criadaEm()) && !c.quando().isAfter(AGORA));
            }
        }
    }

    @Test
    void asfaltoSempreMaisVotadoQueCalcada() {
        for (String cidade : CIDADES) {
            asfaltoSempreMaisVotadoQueCalcada(cidade);
        }
    }

    private void asfaltoSempreMaisVotadoQueCalcada(String cidade) {
        PacoteCidade pacote = pacoteDe(cidade);
        int menorAsfalto = Integer.MAX_VALUE;
        int maiorCalcada = Integer.MIN_VALUE;
        for (DenunciaPlanejada d : pacote.denuncias()) {
            if (d.categoria() == CategoriaDenuncia.ASFALTO) {
                menorAsfalto = Math.min(menorAsfalto, d.saldo());
            } else if (d.categoria() == CategoriaDenuncia.CALCADA_ACESSIBILIDADE) {
                maiorCalcada = Math.max(maiorCalcada, d.saldo());
            }
        }
        assertTrue(menorAsfalto > maiorCalcada, "sem contraste entre asfalto e calçada");
    }

    @Test
    void situacoesPlanejadasParaADemonstracao() {
        for (String cidade : CIDADES) {
            situacoesPlanejadasParaADemonstracao(cidade);
        }
    }

    private void situacoesPlanejadasParaADemonstracao(String cidade) {
        List<DenunciaPlanejada> denuncias = pacoteDe(cidade).denuncias();
        long resolvidas = denuncias.stream().filter(d -> d.status() == StatusDenuncia.RESOLVIDA).count();
        long emAndamento = denuncias.stream().filter(d -> d.status() == StatusDenuncia.EM_ANDAMENTO).count();
        long resolvidasPelaComunidade = denuncias.stream()
                .filter(d -> d.status() == StatusDenuncia.RESOLVIDA && d.confirmacoes().size() == 3).count();
        long faltandoUma = denuncias.stream()
                .filter(d -> d.status() == StatusDenuncia.ABERTA && d.confirmacoes().size() == 2).count();

        assertEquals(2, resolvidas);
        assertEquals(1, emAndamento);
        assertEquals(1, resolvidasPelaComunidade);
        assertEquals(1, faltandoUma);
    }
}
