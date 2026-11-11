package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.UsuarioDao;
import br.com.fiap.meusdireitos.dao.jdbc.ComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.ConfirmacaoResolucaoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.PerfilDemograficoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.VotoDenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.AlteracaoSenhaRequest;
import br.com.fiap.meusdireitos.dto.request.AtualizacaoUsuarioRequest;
import br.com.fiap.meusdireitos.dto.request.CadastroUsuarioRequest;
import br.com.fiap.meusdireitos.dto.request.ExclusaoContaRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioPublicoResponse;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.exception.ConflitoException;
import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.service.simulacao.SimulacaoService;
import br.com.fiap.meusdireitos.service.validacao.Validador;
import br.com.fiap.meusdireitos.util.SenhaUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio da conta do usuário: cadastro, consulta e atualização.
 */
public class UsuarioService {

    private static final String MSG_EMAIL_EM_USO = "Já existe uma conta com este e-mail.";

    /**
     * Cadastra um novo usuário (fim do onboarding conversacional).
     *
     * @throws br.com.fiap.meusdireitos.exception.ValidacaoException se algum campo for inválido
     * @throws ConflitoException se o e-mail já estiver em uso
     */
    public UsuarioResponse cadastrar(CadastroUsuarioRequest req) {
        Validador.corpoObrigatorio(req);
        Validador validador = new Validador();
        validarDadosPessoais(validador, req.nome(), req.email(), req.cidade(), req.uf());
        validarSenhaNova(validador, "senha", req.senha());
        validador.lancarSeHouverErros();

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome());
        usuario.setEmail(req.email());
        usuario.setCidade(req.cidade());
        usuario.setUf(req.uf());
        usuario.setSenhaHash(SenhaUtil.gerarHash(req.senha()));

        Transacao.executarSemRetorno(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            if (dao.buscarPorEmail(usuario.getEmail()) != null) {
                throw new ConflitoException(MSG_EMAIL_EM_USO);
            }
            dao.inserir(usuario);
        });
        // Depois do cadastro confirmado (outra transação): prepara a "cidade viva". Se falhar, o cadastro vale.
        boolean cidadePreparada = new SimulacaoService().prepararCidade(usuario.getCidade(), usuario.getUf());
        return UsuarioResponse.de(usuario, false, cidadePreparada);
    }

    /** Dados da própria conta. */
    public UsuarioResponse buscarConta(Long idUsuario) {
        return Transacao.consultar(con -> {
            Usuario usuario = new UsuarioDaoJdbc(con).buscarPorId(idUsuario);
            return UsuarioResponse.de(usuario, possuiPerfil(con, idUsuario));
        });
    }

    /**
     * Atualiza nome, e-mail, cidade e UF da própria conta.
     *
     * @throws ConflitoException se o novo e-mail pertencer a outra conta
     */
    public UsuarioResponse atualizarConta(Long idUsuario, AtualizacaoUsuarioRequest req) {
        Validador.corpoObrigatorio(req);
        Validador validador = new Validador();
        validarDadosPessoais(validador, req.nome(), req.email(), req.cidade(), req.uf());
        validador.lancarSeHouverErros();

        return Transacao.executar(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            Usuario usuario = dao.buscarPorId(idUsuario);

            Usuario donoDoEmail = dao.buscarPorEmail(Usuario.normalizarEmail(req.email()));
            if (donoDoEmail != null && !donoDoEmail.getId().equals(idUsuario)) {
                throw new ConflitoException(MSG_EMAIL_EM_USO);
            }

            usuario.setNome(req.nome());
            usuario.setEmail(req.email());
            usuario.setCidade(req.cidade());
            usuario.setUf(req.uf());
            dao.atualizar(usuario);
            return UsuarioResponse.de(usuario, possuiPerfil(con, idUsuario));
        });
    }

    /** Lista pública de usuários (apenas nome, cidade e UF). */
    public List<UsuarioPublicoResponse> listarPublico() {
        return Transacao.consultar(con -> {
            List<UsuarioPublicoResponse> lista = new ArrayList<>();
            for (Usuario usuario : new UsuarioDaoJdbc(con).listarReais()) {
                lista.add(UsuarioPublicoResponse.de(usuario, null));
            }
            return lista;
        });
    }

    /** Dados públicos de um usuário, com o total de denúncias registradas. */
    public UsuarioPublicoResponse buscarPublico(Long idUsuario) {
        return Transacao.consultar(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            Usuario usuario = dao.buscarPorId(idUsuario);
            return UsuarioPublicoResponse.de(usuario, dao.contarDenuncias(idUsuario));
        });
    }

    /**
     * Troca a senha, exigindo a senha atual.
     * Senha atual errada responde 400 no campo "senhaAtual" (e não 401, que encerraria a sessão no front).
     */
    public void alterarSenha(Long idUsuario, AlteracaoSenhaRequest req) {
        Validador.corpoObrigatorio(req);
        Validador validador = new Validador().obrigatorio("senhaAtual", req.senhaAtual());
        validarSenhaNova(validador, "novaSenha", req.novaSenha());
        validador.lancarSeHouverErros();

        Transacao.executarSemRetorno(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            Usuario usuario = dao.buscarPorId(idUsuario);
            exigirSenhaCorreta(usuario, req.senhaAtual(), "senhaAtual", "Senha atual incorreta.");
            if (SenhaUtil.verificar(req.novaSenha(), usuario.getSenhaHash())) {
                throw new ValidacaoException("Verifique os campos destacados.",
                        List.of(new ErroCampo("novaSenha", "A nova senha deve ser diferente da atual.")));
            }
            usuario.setSenhaHash(SenhaUtil.gerarHash(req.novaSenha()));
            dao.atualizar(usuario);
        });
    }

    /**
     * Exclui a conta e todos os dados da pessoa (direito ao esquecimento, LGPD), exigindo a senha.
     * Ordem, numa única transação: confirmações, comentários e votos (feitos por ela ou nas denúncias
     * dela) → denúncias dela → perfil demográfico → conta.
     */
    public void excluirConta(Long idUsuario, ExclusaoContaRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador().obrigatorio("senha", req.senha()).lancarSeHouverErros();

        Transacao.executarSemRetorno(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            exigirSenhaCorreta(dao.buscarPorId(idUsuario), req.senha(), "senha", "Senha incorreta.");
            new ConfirmacaoResolucaoDaoJdbc(con).excluirDoUsuario(idUsuario);
            new ComentarioDaoJdbc(con).excluirDoUsuario(idUsuario);
            new VotoDenunciaDaoJdbc(con).excluirDoUsuario(idUsuario);
            new DenunciaDaoJdbc(con).excluirDoAutor(idUsuario);
            new PerfilDemograficoDaoJdbc(con).excluir(idUsuario);
            dao.excluir(idUsuario);
        });
    }

    private static void exigirSenhaCorreta(Usuario usuario, String senha, String campo, String mensagem) {
        if (!SenhaUtil.verificar(senha, usuario.getSenhaHash())) {
            throw new ValidacaoException("Verifique os campos destacados.", List.of(new ErroCampo(campo, mensagem)));
        }
    }

    /** Regra de senha: 8 a 64 caracteres, com pelo menos uma letra e um número. */
    private static void validarSenhaNova(Validador v, String campo, String senha) {
        v.obrigatorio(campo, senha)
                .tamanho(campo, senha, 8, 64)
                .regra(senha == null || temLetraENumero(senha), campo,
                        "A senha deve ter pelo menos uma letra e um número.");
    }

    private static void validarDadosPessoais(Validador v, String nome, String email, String cidade, String uf) {
        v.obrigatorio("nome", nome).tamanho("nome", nome, 3, 100)
                .obrigatorio("email", email).email("email", email)
                .obrigatorio("cidade", cidade).tamanho("cidade", cidade, 2, 80)
                .obrigatorio("uf", uf).uf("uf", uf);
    }

    private static boolean temLetraENumero(String senha) {
        boolean letra = false;
        boolean numero = false;
        for (char c : senha.toCharArray()) {
            letra |= Character.isLetter(c);
            numero |= Character.isDigit(c);
        }
        return letra && numero;
    }

    /** Verifica se o usuário já respondeu o questionário de "Meus Direitos". */
    static boolean possuiPerfil(Connection con, Long idUsuario) throws SQLException {
        return PerfilService.possuiPerfil(con, idUsuario);
    }
}
