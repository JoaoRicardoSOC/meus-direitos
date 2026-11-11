package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;

import java.time.LocalDateTime;
import java.util.List;

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

        public int saldo() {
            int saldo = 0;
            for (VotoPlanejado voto : votos) {
                saldo += voto.tipo().getPeso();
            }
            return saldo;
        }
    }

    public record VotoPlanejado(int morador, TipoVoto tipo, LocalDateTime quando) {
    }

    public record ComentarioPlanejado(int morador, String texto, LocalDateTime quando) {
    }

    public record ConfirmacaoPlanejada(int morador, LocalDateTime quando) {
    }
}
