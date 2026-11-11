package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.UsuarioDao;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.AtualizacaoUsuarioRequest;
import br.com.fiap.meusdireitos.dto.request.CadastroUsuarioRequest;
import br.com.fiap.meusdireitos.dto.response.UsuarioPublicoResponse;
import br.com.fiap.meusdireitos.dto.response.UsuarioResponse;
import br.com.fiap.meusdireitos.exception.ConflitoException;
import br.com.fiap.meusdireitos.model.Usuario;
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
        validador.obrigatorio("senha", req.senha())
                .tamanho("senha", req.senha(), 8, 64)
                .regra(req.senha() == null || temLetraENumero(req.senha()), "senha",
                        "A senha deve ter pelo menos uma letra e um número.")
                .lancarSeHouverErros();

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome());
        usuario.setEmail(req.email());
        usuario.setCidade(req.cidade());
        usuario.setUf(req.uf());
        usuario.setSenhaHash(SenhaUtil.gerarHash(req.senha()));

        return Transacao.executar(con -> {
            UsuarioDao dao = new UsuarioDaoJdbc(con);
            if (dao.buscarPorEmail(usuario.getEmail()) != null) {
                throw new ConflitoException(MSG_EMAIL_EM_USO);
            }
            dao.inserir(usuario);
            return UsuarioResponse.de(usuario, false);
        });
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
            for (Usuario usuario : new UsuarioDaoJdbc(con).listar()) {
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
