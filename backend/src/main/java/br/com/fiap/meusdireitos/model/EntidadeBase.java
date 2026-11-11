package br.com.fiap.meusdireitos.model;

import java.util.Objects;

/**
 * Base das entidades que têm um código próprio gerado pelo banco (sequence).
 * Centraliza o identificador e a comparação entre objetos (herança).
 */
public abstract class EntidadeBase {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** @return true enquanto a entidade ainda não foi gravada no banco */
    public boolean isNova() {
        return id == null;
    }

    /**
     * Duas entidades são iguais quando são do mesmo tipo e têm o mesmo id.
     * Entidades ainda não gravadas (sem id) só são iguais a si mesmas.
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null || getClass() != outro.getClass() || id == null) {
            return false;
        }
        return id.equals(((EntidadeBase) outro).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(getClass(), id);
    }
}
