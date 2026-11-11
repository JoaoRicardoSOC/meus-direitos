package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.IconeDireito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tema de direitos exibido em "Meus Direitos" (tabela TB_DIREITO), por exemplo
 * "Transporte e mobilidade", com os passos para verificar o direito e as tags do público.
 * <p>
 * Os textos usam linguagem cautelosa ("possibilidade a verificar"): a plataforma orienta,
 * mas quem confirma o direito é o órgão oficial.
 */
public class Direito extends EntidadeBase {

    private String titulo;
    private String resumo;
    private String descricao;
    private String aviso;
    private String baseLegal;
    private String linkOficial;
    private IconeDireito icone = IconeDireito.OUTRO;
    private int ordem;
    private final List<PassoDireito> passos = new ArrayList<>();
    private final Set<TagDemografica> tags = EnumSet.noneOf(TagDemografica.class);

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = aparar(titulo);
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = aparar(resumo);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = aparar(descricao);
    }

    public String getAviso() {
        return aviso;
    }

    public void setAviso(String aviso) {
        this.aviso = vazioParaNulo(aviso);
    }

    public String getBaseLegal() {
        return baseLegal;
    }

    public void setBaseLegal(String baseLegal) {
        this.baseLegal = vazioParaNulo(baseLegal);
    }

    public String getLinkOficial() {
        return linkOficial;
    }

    public void setLinkOficial(String linkOficial) {
        this.linkOficial = vazioParaNulo(linkOficial);
    }

    public IconeDireito getIcone() {
        return icone;
    }

    public void setIcone(IconeDireito icone) {
        this.icone = icone == null ? IconeDireito.OUTRO : icone;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    /** @return passos em ordem, somente leitura (use {@link #adicionarPasso}) */
    public List<PassoDireito> getPassos() {
        return Collections.unmodifiableList(passos);
    }

    /** Acrescenta um passo ao final, numerando-o automaticamente. */
    public void adicionarPasso(String titulo, String texto) {
        passos.add(new PassoDireito(passos.size() + 1, aparar(titulo), aparar(texto)));
    }

    /** Usado pela DAO ao carregar do banco, mantendo a ordem gravada. */
    public void adicionarPassoCarregado(PassoDireito passo) {
        passos.add(passo);
    }

    public void limparPassos() {
        passos.clear();
    }

    /** @return tags do público do tema, somente leitura */
    public Set<TagDemografica> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void adicionarTag(TagDemografica tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    public void limparTags() {
        tags.clear();
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
