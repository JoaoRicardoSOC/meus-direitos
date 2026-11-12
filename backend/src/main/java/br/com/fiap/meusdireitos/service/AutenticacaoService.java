package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.LoginRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.exception.MuitasTentativasException;
import br.com.fiap.meusdireitos.exception.NaoAutenticadoException;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.service.validacao.Validador;
import br.com.fiap.meusdireitos.util.SenhaUtil;
import br.com.fiap.meusdireitos.util.TokenUtil;

/**
 * Login e identificação do usuário nas rotas que exigem conta.
 * <p>
 * O login (e o cadastro) devolvem um token assinado ({@link TokenUtil}), válido por 7 dias. O front-end
 * o envia no cabeçalho {@code Authorization: Bearer <token>}; cada rota protegida confere a assinatura,
 * a validade e se o usuário ainda existe. Tentativas de login são limitadas por e-mail
 * ({@link LimiteTentativasLogin}).
 */
public class AutenticacaoService {

    /** Mensagem única para e-mail inexistente e senha errada: não revela qual dos dois falhou. */
    private static final String MSG_LOGIN_INVALIDO = "E-mail ou senha inválidos.";
    private static final String MSG_SEM_SESSAO = "Faça login para continuar.";
    private static final String MSG_SESSAO_INVALIDA = "Sua sessão não é mais válida. Entre novamente.";
    private static final String PREFIXO_BEARER = "Bearer ";

    /** Hash de uma senha aleatória, usado quando o e-mail não existe (ver {@link #entrar}). */
    private static final String HASH_FICTICIO = SenhaUtil.gerarHash(java.util.UUID.randomUUID().toString());

    /** Compartilhado por todas as instâncias (cada resource cria a sua). */
    private static final LimiteTentativasLogin LIMITE = new LimiteTentativasLogin();

    /**
     * Confere e-mail e senha e devolve a conta com o token de sessão.
     *
     * @throws NaoAutenticadoException   se o e-mail não existir ou a senha não conferir
     * @throws MuitasTentativasException se o e-mail estiver bloqueado por excesso de falhas
     */
    public UsuarioResponse entrar(LoginRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador()
                .obrigatorio("email", req.email())
                .obrigatorio("senha", req.senha())
                .lancarSeHouverErros();

        String email = Usuario.normalizarEmail(req.email());
        LIMITE.verificar(email);

        UsuarioResponse resposta = Transacao.consultar(con -> {
            Usuario usuario = new UsuarioDaoJdbc(con).buscarPorEmail(email);
            // Calcula o hash mesmo sem usuário: o tempo de resposta não revela se o e-mail tem conta
            String hashParaComparar = usuario == null ? HASH_FICTICIO : usuario.getSenhaHash();
            boolean senhaConfere = SenhaUtil.verificar(req.senha(), hashParaComparar);
            if (usuario == null || !senhaConfere) {
                return null;
            }
            return UsuarioResponse.de(usuario, UsuarioService.possuiPerfil(con, usuario.getId()));
        });
        if (resposta == null) {
            LIMITE.registrarFalha(email);
            throw new NaoAutenticadoException(MSG_LOGIN_INVALIDO);
        }
        LIMITE.registrarSucesso(email);
        return resposta.comToken(TokenUtil.gerar(resposta.id()));
    }

    /**
     * Para rotas públicas que mostram algo a mais a quem está logado (ex.: o próprio voto).
     * Não consulta o banco: basta o token ser válido.
     *
     * @param cabecalho valor do cabeçalho Authorization (pode ser nulo)
     * @return id de um token válido, ou null se ausente ou inválido (nunca lança exceção)
     */
    public Long idOpcional(String cabecalho) {
        String token = extrairBearer(cabecalho);
        return token == null ? null : TokenUtil.validar(token);
    }

    /**
     * Resolve o usuário logado a partir do cabeçalho {@code Authorization: Bearer <token>}.
     *
     * @param cabecalho valor recebido no cabeçalho Authorization (pode ser nulo)
     * @return id de um usuário existente
     * @throws NaoAutenticadoException se o cabeçalho faltar, o token for inválido ou expirado,
     *                                 ou o usuário não existir mais
     */
    public Long idDoUsuarioLogado(String cabecalho) {
        String token = extrairBearer(cabecalho);
        if (token == null) {
            throw new NaoAutenticadoException(MSG_SEM_SESSAO);
        }
        Long id = TokenUtil.validar(token);
        if (id == null) {
            throw new NaoAutenticadoException(MSG_SESSAO_INVALIDA);
        }
        boolean existe = Transacao.consultar(con -> new UsuarioDaoJdbc(con).existe(id));
        if (!existe) {
            throw new NaoAutenticadoException(MSG_SESSAO_INVALIDA);
        }
        return id;
    }

    /**
     * @return o token depois de "Bearer " (sem diferenciar maiúsculas no esquema), ou null se não houver
     */
    static String extrairBearer(String cabecalho) {
        if (cabecalho == null) {
            return null;
        }
        String valor = cabecalho.trim();
        if (valor.length() <= PREFIXO_BEARER.length()
                || !valor.regionMatches(true, 0, PREFIXO_BEARER, 0, PREFIXO_BEARER.length())) {
            return null;
        }
        String token = valor.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
