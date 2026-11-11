package br.com.fiap.meusdireitos.model;

/**
 * Denúncia como aparece nas telas: com placar de votos, número de comentários e,
 * no feed, se é especialmente relevante para quem está vendo.
 * <p>
 * É um modelo de leitura (resultado de uma consulta com JOIN e GROUP BY), não uma tabela.
 */
public class DenunciaDetalhada {

    private final Denuncia denuncia;
    private final Placar placar;
    private final int comentarios;
    private final int confirmacoesResolucao;
    private final boolean confirmadaPeloLeitor;
    private final Boolean relevanteParaLeitor;

    /**
     * @param confirmacoesResolucao moradores que confirmaram "já foi resolvido"
     * @param confirmadaPeloLeitor  true se quem está vendo já confirmou
     * @param relevanteParaLeitor   preenchido só no feed; null nas demais consultas
     */
    public DenunciaDetalhada(Denuncia denuncia, Placar placar, int comentarios, int confirmacoesResolucao,
                             boolean confirmadaPeloLeitor, Boolean relevanteParaLeitor) {
        this.denuncia = denuncia;
        this.placar = placar;
        this.comentarios = comentarios;
        this.confirmacoesResolucao = confirmacoesResolucao;
        this.confirmadaPeloLeitor = confirmadaPeloLeitor;
        this.relevanteParaLeitor = relevanteParaLeitor;
    }

    public int getConfirmacoesResolucao() {
        return confirmacoesResolucao;
    }

    public boolean isConfirmadaPeloLeitor() {
        return confirmadaPeloLeitor;
    }

    public Denuncia getDenuncia() {
        return denuncia;
    }

    public Placar getPlacar() {
        return placar;
    }

    public int getComentarios() {
        return comentarios;
    }

    public Boolean getRelevanteParaLeitor() {
        return relevanteParaLeitor;
    }
}
