package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.PerfilDemograficoDao;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.DireitoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.PerfilDemograficoDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.PerfilRequest;
import br.com.fiap.meusdireitos.dto.response.DireitoResumoResponse;
import br.com.fiap.meusdireitos.dto.response.OpcaoResponse;
import br.com.fiap.meusdireitos.dto.response.PerfilCompletoResponse;
import br.com.fiap.meusdireitos.dto.response.PerfilResponse;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.model.Direito;
import br.com.fiap.meusdireitos.model.PerfilDemografico;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Questionário "Meus Direitos": grava as respostas e cruza as tags do perfil com o catálogo de direitos.
 */
public class PerfilService {

    static final int IDADE_MAXIMA = 120;
    private static final String MSG_SEM_PERFIL = "Você ainda não respondeu o questionário de Meus Direitos.";

    /**
     * @throws EntidadeNaoEncontradaException (404) se o usuário ainda não respondeu: o front mostra o questionário
     */
    public PerfilCompletoResponse obter(Long idUsuario) {
        return Transacao.consultar(con -> {
            PerfilDemografico perfil = new PerfilDemograficoDaoJdbc(con).buscar(idUsuario);
            if (perfil == null) {
                throw new EntidadeNaoEncontradaException(MSG_SEM_PERFIL);
            }
            return montarResposta(con, perfil);
        });
    }

    /** Cria ou atualiza as respostas e devolve as tags e os direitos aplicáveis. */
    public PerfilCompletoResponse salvar(Long idUsuario, PerfilRequest req) {
        Validador.corpoObrigatorio(req);
        LocalDate hoje = LocalDate.now();
        new Validador()
                .dataNoPassado("dataNascimento", req.dataNascimento())
                .regra(req.dataNascimento() == null || !req.dataNascimento().isBefore(hoje.minusYears(IDADE_MAXIMA)),
                        "dataNascimento", "Confira o ano de nascimento.")
                .lancarSeHouverErros();

        PerfilDemografico perfil = new PerfilDemografico();
        perfil.setIdUsuario(idUsuario);
        perfil.setDataNascimento(req.dataNascimento());
        perfil.setRacaCor(req.racaCor());
        perfil.setDeficiencia(req.deficiencia());
        perfil.setPercursoEscolar(req.percursoEscolar());
        perfil.setBaixaRenda(req.baixaRenda());

        return Transacao.executar(con -> {
            new PerfilDemograficoDaoJdbc(con).salvar(perfil);
            return montarResposta(con, perfil);
        });
    }

    /** Apaga os dados sensíveis do questionário. */
    public void excluir(Long idUsuario) {
        Transacao.executarSemRetorno(con -> {
            if (!new PerfilDemograficoDaoJdbc(con).excluir(idUsuario)) {
                throw new EntidadeNaoEncontradaException(MSG_SEM_PERFIL);
            }
        });
    }

    /**
     * Temas aplicáveis ao usuário logado. Sem perfil respondido, devolve lista vazia
     * (o front-end convida a pessoa a responder o questionário).
     */
    public List<DireitoResumoResponse> direitosDoUsuario(Long idUsuario) {
        return Transacao.consultar(con -> {
            PerfilDemografico perfil = new PerfilDemograficoDaoJdbc(con).buscar(idUsuario);
            if (perfil == null) {
                return List.of();
            }
            return direitosPara(con, perfil.calcularTags(LocalDate.now()));
        });
    }

    /** Usado por outros services (ex.: feed) para saber se o usuário já respondeu. */
    static boolean possuiPerfil(Connection con, Long idUsuario) throws SQLException {
        PerfilDemograficoDao dao = new PerfilDemograficoDaoJdbc(con);
        return dao.existe(idUsuario);
    }

    private PerfilCompletoResponse montarResposta(Connection con, PerfilDemografico perfil) throws SQLException {
        LocalDate hoje = LocalDate.now();
        Set<TagDemografica> tags = perfil.calcularTags(hoje);
        List<OpcaoResponse> rotulos = new ArrayList<>();
        for (TagDemografica tag : tags) {
            rotulos.add(OpcaoResponse.de(tag));
        }
        return new PerfilCompletoResponse(PerfilResponse.de(perfil, hoje), rotulos, direitosPara(con, tags));
    }

    private List<DireitoResumoResponse> direitosPara(Connection con, Set<TagDemografica> tags) throws SQLException {
        List<DireitoResumoResponse> direitos = new ArrayList<>();
        for (Direito direito : new DireitoDaoJdbc(con).listarResumoPorTags(tags)) {
            direitos.add(DireitoResumoResponse.de(direito));
        }
        return direitos;
    }
}
