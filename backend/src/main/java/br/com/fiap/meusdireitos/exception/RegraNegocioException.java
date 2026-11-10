package br.com.fiap.meusdireitos.exception;

/**
 * Os dados são válidos, mas a ação viola uma regra de negócio (HTTP 422).
 * Ex.: votar na própria denúncia.
 */
public class RegraNegocioException extends MeusDireitosException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 422;
    }

    @Override
    public String getTitulo() {
        return "Regra de negócio violada";
    }
}
