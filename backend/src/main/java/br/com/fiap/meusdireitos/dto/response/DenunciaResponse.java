package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.DenunciaDetalhada;

import java.time.LocalDateTime;

/**
 * Denúncia como aparece nos cards do feed, no detalhe e em "Minha denúncia".
 *
 * @param resolucao        progresso da confirmação da comunidade ("já foi resolvido")
 * @param relevanteParaMim só no feed: true se a categoria afeta o perfil de quem está vendo
 */
public record DenunciaResponse(Long id, String protocolo, String titulo, String descricao, OpcaoResponse categoria,
                               OpcaoResponse status, String endereco, String bairro, String cidade, String uf,
                               String imagemUrl, AutorResponse autor, PlacarResponse placar, int comentarios,
                               ResolucaoResponse resolucao, Boolean relevanteParaMim, LocalDateTime criadaEm,
                               LocalDateTime atualizadaEm) {

    public static DenunciaResponse de(DenunciaDetalhada detalhada) {
        Denuncia d = detalhada.getDenuncia();
        ResolucaoResponse resolucao = new ResolucaoResponse(detalhada.getConfirmacoesResolucao(),
                Denuncia.CONFIRMACOES_PARA_RESOLVER, detalhada.isConfirmadaPeloLeitor());
        return new DenunciaResponse(d.getId(), d.getProtocolo(), d.getTitulo(), d.getDescricao(),
                OpcaoResponse.de(d.getCategoria()), OpcaoResponse.de(d.getStatus()), d.getEndereco(), d.getBairro(),
                d.getCidade(), d.getUf(), d.getImagemUrl(), new AutorResponse(d.getIdAutor(), d.getNomeAutor(), d.isAutorSimulado()),
                PlacarResponse.de(detalhada.getPlacar()), detalhada.getComentarios(), resolucao,
                detalhada.getRelevanteParaLeitor(), d.getDataCriacao(), d.getDataAtualizacao());
    }
}
