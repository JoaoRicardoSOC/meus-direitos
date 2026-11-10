package br.com.fiap.meusdireitos.exception;

/**
 * Usuário identificado, mas sem permissão para a ação (HTTP 403).
 * Ex.: editar a denúncia de outra pessoa.
 */
public class AcessoNegadoException extends MeusDireitosException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 403;
    }

    @Override
    public String getTitulo() {
        return "Acesso negado";
    }
}
