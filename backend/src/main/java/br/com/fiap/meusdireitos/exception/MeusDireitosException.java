package br.com.fiap.meusdireitos.exception;

import java.util.List;

/**
 * Base de todas as exceções de negócio da aplicação.
 * <p>
 * Cada subclasse informa o status HTTP e um título curto (polimorfismo). Assim o
 * {@code MeusDireitosExceptionMapper} transforma qualquer uma delas no JSON de erro padrão,
 * sem precisar de try/catch em cada endpoint.
 * <p>
 * É unchecked ({@link RuntimeException}) para atravessar as camadas service e resource
 * sem poluir as assinaturas com {@code throws}.
 */
public abstract class MeusDireitosException extends RuntimeException {

    protected MeusDireitosException(String mensagem) {
        super(mensagem);
    }

    protected MeusDireitosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** @return código HTTP que representa este erro (ex.: 404) */
    public abstract int getStatusHttp();

    /** @return título curto do erro (ex.: "Recurso não encontrado") */
    public abstract String getTitulo();

    /** @return erros por campo, quando houver (por padrão, nenhum) */
    public List<ErroCampo> getCampos() {
        return List.of();
    }
}
