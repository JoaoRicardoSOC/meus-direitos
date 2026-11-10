package br.com.fiap.meusdireitos.exception;

/**
 * A operação conflita com dados existentes (HTTP 409).
 * Ex.: e-mail já cadastrado, ou exclusão de registro que possui vínculos.
 */
public class ConflitoException extends MeusDireitosException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 409;
    }

    @Override
    public String getTitulo() {
        return "Conflito";
    }
}
