package br.com.fiap.meusdireitos.exception;

import java.util.List;

/**
 * Dados enviados pelo cliente são inválidos (HTTP 400). Pode carregar erros por campo.
 */
public class ValidacaoException extends MeusDireitosException {

    private final List<ErroCampo> campos;

    public ValidacaoException(String mensagem) {
        this(mensagem, List.of());
    }

    public ValidacaoException(String mensagem, List<ErroCampo> campos) {
        super(mensagem);
        this.campos = List.copyOf(campos);
    }

    @Override
    public int getStatusHttp() {
        return 400;
    }

    @Override
    public String getTitulo() {
        return "Dados inválidos";
    }

    @Override
    public List<ErroCampo> getCampos() {
        return campos;
    }
}
