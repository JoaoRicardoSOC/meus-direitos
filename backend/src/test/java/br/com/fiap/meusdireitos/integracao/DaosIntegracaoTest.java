package br.com.fiap.meusdireitos.integracao;

import br.com.fiap.meusdireitos.dao.FiltroDenuncia;
import br.com.fiap.meusdireitos.dao.jdbc.ComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.ConfirmacaoResolucaoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.PerfilDemograficoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.RelatorioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.ReporteComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.VotoDenunciaDaoJdbc;
import br.com.fiap.meusdireitos.exception.EntidadeNaoEncontradaException;
import br.com.fiap.meusdireitos.factory.ConnectionFactory;
import br.com.fiap.meusdireitos.model.Comentario;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.DenunciaDetalhada;
import br.com.fiap.meusdireitos.model.Pagina;
import br.com.fiap.meusdireitos.model.Placar;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.VotoDenuncia;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.OrdemFeed;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;
import br.com.fiap.meusdireitos.model.relatorio.ResumoCidade;
import br.com.fiap.meusdireitos.service.ComentarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testes de integração dos DAOs contra o Oracle de verdade.
 * <p>
 * <b>Nada fica gravado:</b> cada teste usa UMA conexão com {@code autoCommit=false} e termina com
 * ROLLBACK (em {@link #desfazer()}, e de novo no {@code close()} do pool). Nenhum teste faz COMMIT
 * nem DDL. Só as sequences avançam (o Oracle não devolve números de sequence no ROLLBACK).
 * <p>
 * Só rodam com a variável de ambiente {@code MD_INTEGRACAO=true}; sem ela, o {@code mvn test} os ignora.
 * Os dados usam uma cidade fictícia com sufixo aleatório, para não se misturar com os dados reais.
 */
class DaosIntegracaoTest {

    private static final String UF = "SP";

    private Connection conexao;
    private String sufixo;
    /** Cidade gravada com acento e espaço repetido; as consultas usam a versão sem acento. */
    private String cidadeComAcento;
    private String cidadeSemAcento;

    @BeforeEach
    void abrirTransacao() throws SQLException {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MD_INTEGRACAO")),
                "Testes de integração desligados (defina MD_INTEGRACAO=true para rodar contra o Oracle)");
        sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        // Gravada como o UsuarioService grava (espaços já normalizados); a busca vem com espaços e sem acento
        cidadeComAcento = "São Integração " + sufixo;
        cidadeSemAcento = "sao integracao " + sufixo.toLowerCase();
        conexao = ConnectionFactory.getConnection();
        conexao.setAutoCommit(false);
    }

    @AfterEach
    void desfazer() throws SQLException {
        if (conexao != null) {
            try {
                conexao.rollback();
            } finally {
                conexao.close();
            }
        }
    }

    @Test
    void inserirEBuscarUsuario() throws SQLException {
        Usuario novo = usuario("Ana");
        UsuarioDaoJdbc dao = new UsuarioDaoJdbc(conexao);
        dao.inserir(novo);
        assertNotNull(novo.getId());

        Usuario lido = dao.buscarPorId(novo.getId());
        assertEquals(novo.getEmail(), lido.getEmail());
        assertEquals(cidadeComAcento, lido.getCidade());
        assertEquals(UF, lido.getUf());
        assertFalse(lido.isSimulado());
        assertEquals(novo.getId(), dao.buscarPorEmail(novo.getEmail()).getId());
        assertTrue(dao.existe(novo.getId()));
    }

    @Test
    void denunciaApareceNoFeedENaPesquisaComCidadeSemAcento() throws SQLException {
        Usuario autor = inserirUsuario("Autor");
        Denuncia denuncia = inserirDenuncia(autor);
        DenunciaDaoJdbc dao = new DenunciaDaoJdbc(conexao);

        Pagina<DenunciaDetalhada> feed = dao.feed(cidadeSemAcento, UF, null, List.of(CategoriaDenuncia.ASFALTO),
                OrdemFeed.PARA_VOCE, 10, 15, 1, 10);
        assertEquals(1, feed.total());
        assertEquals(denuncia.getId(), feed.itens().get(0).getDenuncia().getId());
        assertTrue(feed.itens().get(0).getRelevanteParaLeitor());

        FiltroDenuncia filtro = new FiltroDenuncia("  " + cidadeSemAcento.toUpperCase() + " ", "sp", null, null,
                "buraco " + sufixo.toLowerCase(), null);
        Pagina<DenunciaDetalhada> pesquisa = dao.pesquisar(filtro, autor.getId(), 1, 10);
        assertEquals(1, pesquisa.total());
        assertEquals(denuncia.getId(), pesquisa.itens().get(0).getDenuncia().getId());
    }

    @Test
    void votarEConferirOPlacar() throws SQLException {
        Usuario autor = inserirUsuario("Autor");
        Usuario a = inserirUsuario("Vizinha A");
        Usuario b = inserirUsuario("Vizinho B");
        Denuncia denuncia = inserirDenuncia(autor);
        VotoDenunciaDaoJdbc votos = new VotoDenunciaDaoJdbc(conexao);

        votos.salvar(new VotoDenuncia(a.getId(), denuncia.getId(), TipoVoto.UP));
        votos.salvar(new VotoDenuncia(b.getId(), denuncia.getId(), TipoVoto.DOWN));
        Placar placar = votos.placar(denuncia.getId(), a.getId());
        assertEquals(1, placar.upvotes());
        assertEquals(1, placar.downvotes());
        assertEquals(TipoVoto.UP, placar.meuVoto());

        votos.salvar(new VotoDenuncia(b.getId(), denuncia.getId(), TipoVoto.UP)); // troca o voto
        placar = votos.placar(denuncia.getId(), null);
        assertEquals(2, placar.upvotes());
        assertEquals(0, placar.downvotes());
        assertEquals(2, placar.saldo());
        assertNull(placar.meuVoto());
        assertEquals(2, new DenunciaDaoJdbc(conexao).buscarDetalhada(denuncia.getId(), null).getPlacar().upvotes());
    }

    @Test
    void comentarioComTresReportesSomeDaListagemEDaContagem() throws SQLException {
        Usuario autor = inserirUsuario("Autor");
        Denuncia denuncia = inserirDenuncia(autor);
        ComentarioDaoJdbc comentarios = new ComentarioDaoJdbc(conexao);
        ReporteComentarioDaoJdbc reportes = new ReporteComentarioDaoJdbc(conexao);
        DenunciaDaoJdbc denuncias = new DenunciaDaoJdbc(conexao);

        Comentario comentario = comentar(autor, denuncia, "Comentário que será reportado " + sufixo);
        comentar(autor, denuncia, "Comentário que continua visível " + sufixo);
        assertEquals(2, comentarios.listarPorDenuncia(denuncia.getId()).size());

        for (int i = 1; i < ComentarioService.REPORTES_PARA_OCULTAR; i++) {
            assertTrue(reportes.reportar(inserirUsuario("Leitor " + i).getId(), comentario.getId()));
        }
        assertEquals(2, comentarios.listarPorDenuncia(denuncia.getId()).size(), "abaixo do limite continua visível");

        Usuario ultimo = inserirUsuario("Leitor final");
        assertTrue(reportes.reportar(ultimo.getId(), comentario.getId()));
        assertFalse(reportes.reportar(ultimo.getId(), comentario.getId()), "reportar de novo não conta duas vezes");
        assertEquals(ComentarioService.REPORTES_PARA_OCULTAR, reportes.contar(comentario.getId()));

        List<Comentario> visiveis = comentarios.listarPorDenuncia(denuncia.getId());
        assertEquals(1, visiveis.size());
        assertFalse(visiveis.stream().anyMatch(c -> c.getId().equals(comentario.getId())));
        assertEquals(1, denuncias.buscarDetalhada(denuncia.getId(), null).getComentarios());

        // oculto também para editar e reportar (busca de visíveis), mas o autor ainda o encontra para excluir
        assertThrows(EntidadeNaoEncontradaException.class, () -> comentarios.buscarVisivelPorId(comentario.getId()));
        assertEquals(comentario.getId(), comentarios.buscarPorId(comentario.getId()).getId());

        // excluir o comentário exige apagar os reportes antes (sem ON DELETE CASCADE)
        reportes.excluirPorComentario(comentario.getId());
        comentarios.excluir(comentario.getId());
        assertEquals(0, reportes.contar(comentario.getId()));
    }

    @Test
    void resumoDaCidadeContaSoOsDadosDaCidade() throws SQLException {
        Usuario autor = inserirUsuario("Autor");
        Usuario vizinho = inserirUsuario("Vizinho");
        Denuncia denuncia = inserirDenuncia(autor);
        new VotoDenunciaDaoJdbc(conexao).salvar(new VotoDenuncia(vizinho.getId(), denuncia.getId(), TipoVoto.UP));
        comentar(vizinho, denuncia, "Também passei por esse buraco " + sufixo);

        ResumoCidade resumo = new RelatorioDaoJdbc(conexao).resumo(cidadeSemAcento, UF);
        assertEquals(2, resumo.usuarios());
        assertEquals(1, resumo.denuncias());
        assertEquals(1, resumo.abertas());
        assertEquals(0, resumo.resolvidas());
        assertEquals(1, resumo.votos());
        assertEquals(1, resumo.comentarios());
        assertEquals(0, resumo.denunciasSimuladas());
    }

    @Test
    void exclusaoDeContaApagaOsReportesAntesDosComentarios() throws SQLException {
        Usuario autor = inserirUsuario("Autor");
        Usuario vizinho = inserirUsuario("Vizinho");
        Denuncia denuncia = inserirDenuncia(autor);
        Comentario doVizinho = comentar(vizinho, denuncia, "Comentário do vizinho " + sufixo);
        Comentario doAutor = comentar(autor, denuncia, "Comentário do autor " + sufixo);
        ReporteComentarioDaoJdbc reportes = new ReporteComentarioDaoJdbc(conexao);
        reportes.reportar(autor.getId(), doVizinho.getId());
        reportes.reportar(vizinho.getId(), doAutor.getId());

        // mesma ordem de UsuarioService.excluirConta: não pode dar ORA-02292
        Long id = autor.getId();
        new ConfirmacaoResolucaoDaoJdbc(conexao).excluirDoUsuario(id);
        reportes.excluirDoUsuario(id);
        new ComentarioDaoJdbc(conexao).excluirDoUsuario(id);
        new VotoDenunciaDaoJdbc(conexao).excluirDoUsuario(id);
        new DenunciaDaoJdbc(conexao).excluirDoAutor(id);
        new PerfilDemograficoDaoJdbc(conexao).excluir(id);
        new UsuarioDaoJdbc(conexao).excluir(id);

        assertFalse(new UsuarioDaoJdbc(conexao).existe(id));
        assertEquals(0, reportes.contar(doVizinho.getId()));
        assertEquals(0, reportes.contar(doAutor.getId()));
    }

    // ------------------------------------------------------------------ auxiliares

    private Usuario usuario(String nome) {
        Usuario u = new Usuario();
        u.setNome(nome + " Teste " + sufixo);
        u.setEmail("integracao." + UUID.randomUUID().toString().substring(0, 8) + "." + sufixo.toLowerCase()
                + "@teste.invalid");
        u.setSenhaHash("sem-login-teste-integracao");
        u.setCidade(cidadeComAcento);
        u.setUf(UF);
        return u;
    }

    private Usuario inserirUsuario(String nome) throws SQLException {
        return new UsuarioDaoJdbc(conexao).inserir(usuario(nome));
    }

    private Denuncia inserirDenuncia(Usuario autor) throws SQLException {
        Denuncia d = new Denuncia();
        d.setIdAutor(autor.getId());
        d.setTitulo("Buraco " + sufixo + " na rua de teste");
        d.setDescricao("Denúncia criada por teste de integração; desfeita com ROLLBACK.");
        d.setCategoria(CategoriaDenuncia.ASFALTO);
        d.setEndereco("Rua do Teste, 100");
        d.setBairro("Centro");
        d.setCidade(autor.getCidade());
        d.setUf(autor.getUf());
        return new DenunciaDaoJdbc(conexao).inserir(d);
    }

    private Comentario comentar(Usuario autor, Denuncia denuncia, String texto) throws SQLException {
        Comentario c = new Comentario();
        c.setIdDenuncia(denuncia.getId());
        c.setIdAutor(autor.getId());
        c.setTexto(texto);
        return new ComentarioDaoJdbc(conexao).inserir(c);
    }
}
