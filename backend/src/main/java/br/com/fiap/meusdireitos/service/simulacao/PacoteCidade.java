package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.OrigemStatus;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Plano do conteúdo de demonstração de uma cidade, gerado antes de gravar no banco.
 * Os participantes são indicados pela posição na lista {@code moradores}.
 */
public record PacoteCidade(List<String> moradores, List<DenunciaPlanejada> denuncias) {

    public record DenunciaPlanejada(int autor, CategoriaDenuncia categoria, String titulo, String descricao,
                                    String endereco, String bairro, String imagemUrl, LocalDateTime criadaEm,
                                    StatusDenuncia status, List<VotoPlanejado> votos,
                                    List<ComentarioPlanejado> comentarios,
                                    List<ConfirmacaoPlanejada> confirmacoes) {

        /**
         * Mudança de situação que levou a denúncia de exemplo do estado ABERTA ao atual, para a linha do
         * tempo. Resolvida com 3 confirmações = pela comunidade, no momento da última confirmação; resolvida
         * ou em andamento sem isso = pelo autor, no meio do caminho entre o registro e a última atividade.
         *
         * @return a mudança, ou vazio se a denúncia continua ABERTA
         */
        public Optional<MudancaPlanejada> mudancaDeSituacao() {
            if (status == StatusDenuncia.ABERTA) {
                return Optional.empty();
            }
            if (status == StatusDenuncia.RESOLVIDA && confirmacoes.size() >= GeradorConteudo.CONFIRMACOES_PARA_RESOLVER) {
                LocalDateTime ultima = confirmacoes.stream().map(ConfirmacaoPlanejada::quando)
                        .max(Comparator.naturalOrder()).orElse(criadaEm);
                return Optional.of(new MudancaPlanejada(status, OrigemStatus.COMUNIDADE, ultima));
            }
            LocalDateTime ultimaAtividade = criadaEm;
            for (VotoPlanejado v : votos) {
                ultimaAtividade = v.quando().isAfter(ultimaAtividade) ? v.quando() : ultimaAtividade;
            }
            for (ComentarioPlanejado c : comentarios) {
                ultimaAtividade = c.quando().isAfter(ultimaAtividade) ? c.quando() : ultimaAtividade;
            }
            long meio = Duration.between(criadaEm, ultimaAtividade).toMinutes() / 2;
            return Optional.of(new MudancaPlanejada(status, OrigemStatus.AUTOR, criadaEm.plusMinutes(meio)));
        }

        public int saldo() {
            int saldo = 0;
            for (VotoPlanejado voto : votos) {
                saldo += voto.tipo().getPeso();
            }
            return saldo;
        }
    }

    /** Mudança de ABERTA para {@code nova}, vinda do autor ou da comunidade. */
    public record MudancaPlanejada(StatusDenuncia nova, OrigemStatus origem, LocalDateTime quando) {
    }

    public record VotoPlanejado(int morador, TipoVoto tipo, LocalDateTime quando) {
    }

    public record ComentarioPlanejado(int morador, String texto, LocalDateTime quando) {
    }

    public record ConfirmacaoPlanejada(int morador, LocalDateTime quando) {
    }
}
