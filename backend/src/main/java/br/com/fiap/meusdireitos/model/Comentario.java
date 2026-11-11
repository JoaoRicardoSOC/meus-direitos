package br.com.fiap.meusdireitos.model;

import java.time.LocalDateTime;

/**
 * Mensagem da "Conversa do bairro" em uma denúncia (tabela TB_COMENTARIO).
 */
public class Comentario extends EntidadeBase {

    private Long idDenuncia;
    private Long idAutor;
    private String nomeAutor;
    private String texto;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEdicao;

    /** @return true se o usuário informado escreveu o comentário */
    public boolean isAutor(Long idUsuario) {
        return idAutor != null && idAutor.equals(idUsuario);
    }

    public Long getIdDenuncia() {
        return idDenuncia;
    }

    public void setIdDenuncia(Long idDenuncia) {
        this.idDenuncia = idDenuncia;
    }

    public Long getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(Long idAutor) {
        this.idAutor = idAutor;
    }

    public String getNomeAutor() {
        return nomeAutor;
    }

    public void setNomeAutor(String nomeAutor) {
        this.nomeAutor = nomeAutor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto == null ? null : texto.trim();
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataEdicao() {
        return dataEdicao;
    }

    public void setDataEdicao(LocalDateTime dataEdicao) {
        this.dataEdicao = dataEdicao;
    }
}
