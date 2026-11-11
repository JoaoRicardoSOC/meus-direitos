package br.com.fiap.meusdireitos.model.relatorio;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

/**
 * Denúncia com mais participação da comunidade.
 *
 * @param votos       total de votos recebidos (apoios + discordâncias)
 * @param saldo       apoios menos discordâncias
 * @param comentarios mensagens na conversa do bairro
 */
public record DenunciaEngajamento(Long id, String titulo, CategoriaDenuncia categoria, StatusDenuncia status,
                                  int votos, int saldo, int comentarios) {

    /** @return pontuação de engajamento: cada voto e cada comentário contam 1 */
    public int engajamento() {
        return votos + comentarios;
    }
}
