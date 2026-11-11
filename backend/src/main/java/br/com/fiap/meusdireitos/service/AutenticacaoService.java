package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.exception.NaoAutenticadoException;

/**
 * Login e identificação do usuário nas rotas que exigem conta.
 * <p>
 * MVP: depois do login o front-end envia o id do usuário no cabeçalho {@code X-Usuario-Id}.
 * Limitação conhecida (documentada no README): o cabeçalho não é assinado; o próximo passo
 * seria um token assinado. Mesmo assim, toda rota confere se o usuário existe.
 */
public class AutenticacaoService {

    private static final String MSG_SEM_SESSAO = "Faça login para continuar.";

    /**
     * Resolve o usuário logado a partir do cabeçalho X-Usuario-Id.
     *
     * @param cabecalho valor recebido no cabeçalho (pode ser nulo)
     * @return id de um usuário existente
     * @throws NaoAutenticadoException se o cabeçalho faltar, for inválido ou o usuário não existir
     */
    public Long idDoUsuarioLogado(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            throw new NaoAutenticadoException(MSG_SEM_SESSAO);
        }
        long id;
        try {
            id = Long.parseLong(cabecalho.trim());
        } catch (NumberFormatException e) {
            throw new NaoAutenticadoException(MSG_SEM_SESSAO);
        }
        boolean existe = Transacao.consultar(con -> new UsuarioDaoJdbc(con).existe(id));
        if (!existe) {
            throw new NaoAutenticadoException("Sua sessão não é mais válida. Entre novamente.");
        }
        return id;
    }
}
