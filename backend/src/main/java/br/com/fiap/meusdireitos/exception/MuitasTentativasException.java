package br.com.fiap.meusdireitos.exception;

/**
 * Muitas tentativas seguidas em pouco tempo (HTTP 429). Ex.: login bloqueado depois de várias senhas erradas.
 */
public class MuitasTentativasException extends MeusDireitosException {

    public MuitasTentativasException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int getStatusHttp() {
        return 429;
    }

    @Override
    public String getTitulo() {
        return "Muitas tentativas";
    }
}
