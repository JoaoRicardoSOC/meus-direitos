package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.LoginRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.exception.NaoAutenticadoException;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.service.validacao.Validador;
import br.com.fiap.meusdireitos.util.SenhaUtil;

/**
 * Login e identificação do usuário nas rotas que exigem conta.
 * <p>
 * MVP: depois do login o front-end envia o id do usuário no cabeçalho {@code X-Usuario-Id}.
 * Limitação conhecida (documentada no README): o cabeçalho não é assinado; o próximo passo
 * seria um token assinado. Mesmo assim, toda rota confere se o usuário existe.
 */
public class AutenticacaoService {

    /** Mensagem única para e-mail inexistente e senha errada: não revela qual dos dois falhou. */
    private static final String MSG_LOGIN_INVALIDO = "E-mail ou senha inválidos.";
    private static final String MSG_SEM_SESSAO = "Faça login para continuar.";

    /** Hash de uma senha aleatória, usado quando o e-mail não existe (ver {@link #entrar}). */
    private static final String HASH_FICTICIO = SenhaUtil.gerarHash(java.util.UUID.randomUUID().toString());

    /**
     * @throws NaoAutenticadoException se o e-mail não existir ou a senha não conferir
     */
    public UsuarioResponse entrar(LoginRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador()
                .obrigatorio("email", req.email())
                .obrigatorio("senha", req.senha())
                .lancarSeHouverErros();

        return Transacao.consultar(con -> {
            Usuario usuario = new UsuarioDaoJdbc(con).buscarPorEmail(Usuario.normalizarEmail(req.email()));
            // Calcula o hash mesmo sem usuário: o tempo de resposta não revela se o e-mail tem conta
            String hashParaComparar = usuario == null ? HASH_FICTICIO : usuario.getSenhaHash();
            boolean senhaConfere = SenhaUtil.verificar(req.senha(), hashParaComparar);
            if (usuario == null || !senhaConfere) {
                throw new NaoAutenticadoException(MSG_LOGIN_INVALIDO);
            }
            return UsuarioResponse.de(usuario, UsuarioService.possuiPerfil(con, usuario.getId()));
        });
    }

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
