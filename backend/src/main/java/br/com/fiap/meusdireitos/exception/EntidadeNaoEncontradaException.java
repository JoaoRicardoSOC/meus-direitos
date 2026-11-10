package br.com.fiap.meusdireitos.exception;

/**
 * O registro procurado não existe (HTTP 404).
 */
public class EntidadeNaoEncontradaException extends MeusDireitosException {

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 404;
    }

    @Override
    public String getTitulo() {
        return "Recurso não encontrado";
    }
}
