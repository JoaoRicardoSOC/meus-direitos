package br.com.fiap.meusdireitos.exception;

/**
 * Usuário não identificado ou credenciais incorretas (HTTP 401).
 */
public class NaoAutenticadoException extends MeusDireitosException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 401;
    }

    @Override
    public String getTitulo() {
        return "Não autenticado";
    }
}
