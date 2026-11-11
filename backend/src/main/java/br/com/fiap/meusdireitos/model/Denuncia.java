package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

import java.time.LocalDateTime;

/**
 * Problema urbano registrado por um morador (tabela TB_DENUNCIA).
 * <p>
 * A cidade e a UF são copiadas do autor no momento do registro: a denúncia pertence ao lugar,
 * não à pessoa (se o autor se mudar, a denúncia continua na cidade original).
 */
public class Denuncia extends EntidadeBase {

    private Long idAutor;
    private String nomeAutor;
    private String titulo;
    private String descricao;
    private CategoriaDenuncia categoria;
    private StatusDenuncia status = StatusDenuncia.ABERTA;
    private String endereco;
    private String bairro;
    private String cidade;
    private String uf;
    private String imagemUrl;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /**
     * Protocolo exibido ao morador (tela "Sua voz foi ouvida"): ano + id com 4 dígitos.
     *
     * @return ex.: "2026-0142", ou null se ainda não foi gravada
     */
    public String getProtocolo() {
        if (getId() == null || dataCriacao == null) {
            return null;
        }
        return String.format("%d-%04d", dataCriacao.getYear(), getId());
    }

    /** @return true se o usuário informado é o autor (único que pode editar, excluir ou mudar o status) */
    public boolean isAutor(Long idUsuario) {
        return idAutor != null && idAutor.equals(idUsuario);
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = aparar(titulo);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = aparar(descricao);
    }

    public CategoriaDenuncia getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaDenuncia categoria) {
        this.categoria = categoria;
    }

    public StatusDenuncia getStatus() {
        return status;
    }

    public void setStatus(StatusDenuncia status) {
        this.status = status;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = aparar(endereco);
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = vazioParaNulo(bairro);
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = aparar(cidade);
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf == null ? null : uf.trim().toUpperCase();
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = vazioParaNulo(imagemUrl);
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
