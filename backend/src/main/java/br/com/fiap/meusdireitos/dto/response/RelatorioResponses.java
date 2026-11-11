package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.relatorio.DenunciaEngajamento;
import br.com.fiap.meusdireitos.model.relatorio.ResumoCidade;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorBairro;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorCategoria;

/**
 * Respostas dos relatórios da cidade, agrupadas em um só arquivo por serem simples.
 */
public final class RelatorioResponses {

    private RelatorioResponses() {
    }

    /** "Números da cidade". */
    public record Resumo(String cidade, String uf, int usuarios, int denuncias, int abertas, int emAndamento,
                         int resolvidas, double percentualResolvidas, int votos, int comentarios,
                         int confirmacoesDeResolucao, int temasDeDireitos) {

        public static Resumo de(String cidade, String uf, ResumoCidade r) {
            return new Resumo(cidade, uf, r.usuarios(), r.denuncias(), r.abertas(), r.emAndamento(), r.resolvidas(),
                    r.percentualResolvidas(), r.votos(), r.comentarios(), r.confirmacoes(), r.temasDeDireitos());
        }
    }

    public record PorCategoria(OpcaoResponse categoria, int total, int abertas, int emAndamento, int resolvidas,
                               double percentualResolvidas) {

        public static PorCategoria de(TotalPorCategoria t) {
            return new PorCategoria(OpcaoResponse.de(t.categoria()), t.total(), t.abertas(), t.emAndamento(),
                    t.resolvidas(), t.percentualResolvidas());
        }
    }

    public record PorBairro(String bairro, int total, int resolvidas, double percentualResolvidas, int saldoVotos) {

        public static PorBairro de(TotalPorBairro t) {
            return new PorBairro(t.bairro(), t.total(), t.resolvidas(), t.percentualResolvidas(), t.saldoVotos());
        }
    }

    public record Engajamento(Long id, String titulo, OpcaoResponse categoria, OpcaoResponse status, int votos,
                              int saldo, int comentarios, int engajamento) {

        public static Engajamento de(DenunciaEngajamento d) {
            return new Engajamento(d.id(), d.titulo(), OpcaoResponse.de(d.categoria()), OpcaoResponse.de(d.status()),
                    d.votos(), d.saldo(), d.comentarios(), d.engajamento());
        }
    }
}
