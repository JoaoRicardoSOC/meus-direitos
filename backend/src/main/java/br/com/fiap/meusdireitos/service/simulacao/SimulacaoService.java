package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.config.AppConfig;
import br.com.fiap.meusdireitos.dao.ConfirmacaoResolucaoDao;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.UsuarioDao;
import br.com.fiap.meusdireitos.dao.VotoDenunciaDao;
import br.com.fiap.meusdireitos.dao.jdbc.ComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.ConfirmacaoResolucaoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.PerfilDemograficoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.SemeaduraCidadeDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.VotoDenunciaDaoJdbc;
import br.com.fiap.meusdireitos.model.Comentario;
import br.com.fiap.meusdireitos.model.ConfirmacaoResolucao;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.SemeaduraCidade;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.VotoDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ComentarioPlanejado;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ConfirmacaoPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.DenunciaPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.VotoPlanejado;
import br.com.fiap.meusdireitos.util.TextoUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * "Cidade viva": conteúdo de demonstração para que o feed de qualquer cidade já mostre todas as
 * funcionalidades (votos, conversa do bairro, resolução pela comunidade, feed "Para você").
 * <p>
 * Todo conteúdo gerado pertence a moradores marcados como simulados (selo "Exemplo" no front).
 * Os moradores simulados nunca interagem com denúncias de usuários reais e não conseguem fazer login.
 * Falhas aqui nunca derrubam o cadastro nem o feed: são só registradas no log.
 */
public class SimulacaoService {

    private static final Logger LOG = Logger.getLogger(SimulacaoService.class.getName());

    /** "Hash" que nunca confere com nenhuma senha: moradores simulados não fazem login. */
    static final String SEM_LOGIN = "SIMULADO:sem-login";
    static final String DOMINIO_EMAIL = "@simulado.meusdireitos.local";
    static final int MINUTOS_ENTRE_PULSOS = 10;
    static final int MAX_EVENTOS_POR_PULSO = 3;

    private static CatalogoSimulacao catalogo;

    private static synchronized CatalogoSimulacao catalogo() {
        if (catalogo == null) {
            catalogo = CatalogoSimulacao.carregar();
        }
        return catalogo;
    }

    // ================================================================== semeadura

    /**
     * Chamado após o cadastro: garante que a cidade tenha conteúdo de demonstração.
     *
     * @return true se a cidade tem conteúdo (semeada agora ou antes); false se a simulação está
     * desligada ou falhou (o cadastro segue normalmente)
     */
    public boolean prepararCidade(String cidade, String uf) {
        if (!AppConfig.simulacaoAtiva()) {
            return false;
        }
        try {
            semear(cidade, uf);
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Não foi possível preparar a cidade " + cidade + "/" + uf, e);
            return false;
        }
    }

    /**
     * Grava o pacote de demonstração da cidade, uma única vez.
     *
     * @return true se semeou agora; false se a cidade já tinha sido semeada
     */
    public boolean semear(String cidade, String uf) {
        String nomeCidade = TextoUtil.capitalizar(cidade);
        String siglaUf = uf.trim().toUpperCase();
        String chave = TextoUtil.chaveCidade(cidade);
        return Transacao.executar(con -> {
            if (!new SemeaduraCidadeDaoJdbc(con).registrar(chave, siglaUf, nomeCidade)) {
                return false;
            }
            PacoteCidade pacote = new GeradorConteudo(GeradorConteudo.randomDaCidade(chave, siglaUf), catalogo())
                    .gerarPacote(LocalDateTime.now());
            gravarPacote(con, pacote, nomeCidade, siglaUf);
            LOG.info(() -> "Cidade semeada: " + nomeCidade + "/" + siglaUf);
            return true;
        });
    }

    /**
     * Grava o pacote com lotes JDBC (addBatch/executeBatch): em vez de ~130 idas ao banco, uma por
     * tabela, mais uma consulta para recuperar os ids gerados de moradores e denúncias.
     */
    private void gravarPacote(Connection con, PacoteCidade pacote, String cidade, String uf) throws SQLException {
        List<Long> ids = gravarMoradores(new UsuarioDaoJdbc(con), pacote, cidade, uf);
        List<Long> idsDenuncias = gravarDenuncias(new DenunciaDaoJdbc(con), pacote, ids, cidade, uf);

        List<VotoDenuncia> votos = new ArrayList<>();
        List<Comentario> comentarios = new ArrayList<>();
        List<ConfirmacaoResolucao> confirmacoes = new ArrayList<>();
        for (int i = 0; i < pacote.denuncias().size(); i++) {
            DenunciaPlanejada planejada = pacote.denuncias().get(i);
            Long idDenuncia = idsDenuncias.get(i);
            for (VotoPlanejado v : planejada.votos()) {
                VotoDenuncia voto = new VotoDenuncia(ids.get(v.morador()), idDenuncia, v.tipo());
                voto.setDataVoto(v.quando());
                votos.add(voto);
            }
            for (ComentarioPlanejado c : planejada.comentarios()) {
                comentarios.add(comentario(idDenuncia, ids.get(c.morador()), c.texto(), c.quando()));
            }
            for (ConfirmacaoPlanejada c : planejada.confirmacoes()) {
                confirmacoes.add(new ConfirmacaoResolucao(ids.get(c.morador()), idDenuncia, c.quando()));
            }
        }
        new VotoDenunciaDaoJdbc(con).inserirEmLote(votos);
        new ComentarioDaoJdbc(con).inserirEmLote(comentarios);
        new ConfirmacaoResolucaoDaoJdbc(con).confirmarEmLote(confirmacoes);
    }

    /** @return ids dos moradores, na mesma ordem de {@code pacote.moradores()} */
    private List<Long> gravarMoradores(UsuarioDao usuarioDao, PacoteCidade pacote, String cidade, String uf)
            throws SQLException {
        LocalDateTime cadastro = pacote.denuncias().get(0).criadaEm().minusDays(3);
        List<Usuario> moradores = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < pacote.moradores().size(); i++) {
            Usuario morador = morador(pacote.moradores().get(i), i, cidade, uf, cadastro);
            moradores.add(morador);
            emails.add(morador.getEmail());
        }
        usuarioDao.inserirEmLote(moradores);
        Map<String, Long> porEmail = usuarioDao.idsPorEmail(emails);
        List<Long> ids = new ArrayList<>();
        for (String email : emails) {
            ids.add(exigirId(porEmail.get(email), "morador " + email));
        }
        return ids;
    }

    /**
     * Denúncias com par autor+título único vão em lote; uma eventual repetição (raríssima, pois o
     * título leva rua e número) é gravada individualmente, para o id não ser confundido.
     *
     * @return ids das denúncias, na mesma ordem de {@code pacote.denuncias()}
     */
    private List<Long> gravarDenuncias(DenunciaDaoJdbc denunciaDao, PacoteCidade pacote, List<Long> ids,
                                       String cidade, String uf) throws SQLException {
        List<Denuncia> denuncias = new ArrayList<>();
        List<Denuncia> emLote = new ArrayList<>();
        Set<String> chaves = new HashSet<>();
        for (DenunciaPlanejada planejada : pacote.denuncias()) {
            Denuncia d = denuncia(planejada, ids, cidade, uf);
            denuncias.add(d);
            if (chaves.add(DenunciaDaoJdbc.chaveAutorTitulo(d.getIdAutor(), d.getTitulo()))) {
                emLote.add(d);
            }
        }
        denunciaDao.inserirEmLote(emLote);
        Map<String, Long> porChave = denunciaDao.idsPorAutorETitulo(ids);
        for (Denuncia d : emLote) {
            d.setId(exigirId(porChave.get(DenunciaDaoJdbc.chaveAutorTitulo(d.getIdAutor(), d.getTitulo())),
                    "denúncia " + d.getTitulo()));
        }
        List<Long> idsDenuncias = new ArrayList<>();
        for (Denuncia d : denuncias) {
            if (d.getId() == null) {
                denunciaDao.inserir(d);
            }
            idsDenuncias.add(d.getId());
        }
        return idsDenuncias;
    }

    private static Long exigirId(Long id, String descricao) throws SQLException {
        if (id == null) {
            throw new SQLException("Id gerado não encontrado após inserção em lote: " + descricao);
        }
        return id;
    }

    // ================================================================== pulso de atividade

    /**
     * Chamado ao abrir o feed: se a cidade foi semeada e passaram 10 minutos desde a última
     * atividade simulada, gera de 1 a 3 eventos novos. Nunca lança exceção.
     */
    public void pulsarSeNecessario(String cidade, String uf) {
        if (!AppConfig.simulacaoAtiva()) {
            return;
        }
        try {
            pulsar(cidade, uf, false, 0);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Falha no pulso de atividade de " + cidade + "/" + uf, e);
        }
    }

    /**
     * @param forcar  ignora o intervalo mínimo (rota de preparo da demonstração)
     * @param eventos quantidade desejada quando forçado (1 a 10); 0 = sorteia de 1 a 3
     * @return quantidade de eventos gerados (0 se a cidade não foi semeada ou ainda não é hora)
     */
    public int pulsar(String cidade, String uf, boolean forcar, int eventos) {
        String chave = TextoUtil.chaveCidade(cidade);
        String siglaUf = uf.trim().toUpperCase();
        return Transacao.executar(con -> {
            SemeaduraCidadeDaoJdbc semeaduraDao = new SemeaduraCidadeDaoJdbc(con);
            SemeaduraCidade semeadura = semeaduraDao.buscar(chave, siglaUf);
            LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            if (semeadura == null || (!forcar
                    && semeadura.dataUltimoPulso().plusMinutes(MINUTOS_ENTRE_PULSOS).isAfter(agora))) {
                return 0;
            }
            List<Usuario> moradores = new UsuarioDaoJdbc(con).listarSimuladosDaCidade(semeadura.cidade(), siglaUf);
            if (moradores.size() < 2) {
                return 0;
            }
            GeradorConteudo gerador = new GeradorConteudo(new Random(), catalogo());
            int quantidade = eventos > 0 ? Math.min(eventos, 10) : gerador.inteiro(1, MAX_EVENTOS_POR_PULSO);
            int gerados = 0;
            for (int i = 0; i < quantidade; i++) {
                if (gerarEvento(con, gerador, moradores, semeadura, agora)) {
                    gerados++;
                }
            }
            semeaduraDao.registrarPulso(chave, siglaUf, agora, gerados);
            return gerados;
        });
    }

    /** Gera um evento sorteado. Só mexe em denúncias de moradores simulados. */
    private boolean gerarEvento(Connection con, GeradorConteudo gerador, List<Usuario> moradores,
                                SemeaduraCidade cidade, LocalDateTime agora) throws SQLException {
        DenunciaDaoJdbc denunciaDao = new DenunciaDaoJdbc(con);
        List<Denuncia> denuncias = denunciaDao.listarSimuladasDaCidade(cidade.cidade(), cidade.uf());
        GeradorConteudo.TipoEvento tipo = denuncias.isEmpty()
                ? GeradorConteudo.TipoEvento.NOVA_DENUNCIA : gerador.sortearEvento();

        switch (tipo) {
            case NOVA_DENUNCIA: {
                int autor = gerador.inteiro(0, moradores.size() - 1);
                DenunciaPlanejada planejada = gerador.novaDenunciaRecente(agora, autor);
                List<Long> ids = new ArrayList<>();
                for (Usuario morador : moradores) {
                    ids.add(morador.getId());
                }
                denunciaDao.inserir(denuncia(planejada, ids, cidade.cidade(), cidade.uf()));
                return true;
            }
            case COMENTARIO: {
                Denuncia alvo = gerador.escolher(denuncias);
                Usuario morador = outroMorador(gerador, moradores, alvo);
                new ComentarioDaoJdbc(con).inserir(comentario(alvo.getId(), morador.getId(),
                        gerador.comentarioAleatorio(), agora.minusMinutes(gerador.inteiro(0, 5))));
                return true;
            }
            case CONFIRMACAO: {
                ConfirmacaoResolucaoDao confirmacaoDao = new ConfirmacaoResolucaoDaoJdbc(con);
                for (Denuncia alvo : denuncias) {
                    // não completa a denúncia que está com 2 de 3: essa fica para o avaliador
                    int confirmacoes = confirmacaoDao.contar(alvo.getId());
                    if (alvo.aceitaConfirmacaoDeResolucao() && confirmacoes < Denuncia.CONFIRMACOES_PARA_RESOLVER - 1) {
                        return confirmacaoDao.confirmar(outroMorador(gerador, moradores, alvo).getId(), alvo.getId());
                    }
                }
                return false;
            }
            case VOTO:
            default: {
                VotoDenunciaDao votoDao = new VotoDenunciaDaoJdbc(con);
                for (int tentativa = 0; tentativa < 3; tentativa++) {
                    Denuncia alvo = gerador.escolher(denuncias);
                    Usuario morador = outroMorador(gerador, moradores, alvo);
                    if (alvo.getStatus() != StatusDenuncia.RESOLVIDA && !votoDao.jaVotou(morador.getId(), alvo.getId())) {
                        TipoVoto tipoVoto = gerador.inteiro(1, 10) == 1 ? TipoVoto.DOWN : TipoVoto.UP;
                        votoDao.salvar(new VotoDenuncia(morador.getId(), alvo.getId(), tipoVoto));
                        return true;
                    }
                }
                return false;
            }
        }
    }

    // ================================================================== administração

    public List<SemeaduraCidade> listarCidades() {
        return Transacao.consultar(con -> new SemeaduraCidadeDaoJdbc(con).listar());
    }

    /**
     * Apaga todo o conteúdo simulado (moradores, denúncias, votos, comentários, confirmações e o controle
     * por cidade), numa única transação. Dados de usuários reais em denúncias reais não são tocados.
     *
     * @return quantidade de moradores simulados removidos
     */
    public int limparTudo() {
        return Transacao.executar(con -> {
            List<Usuario> simulados = new UsuarioDaoJdbc(con).listarSimulados();
            for (Usuario morador : simulados) {
                Long id = morador.getId();
                new ConfirmacaoResolucaoDaoJdbc(con).excluirDoUsuario(id);
                new ComentarioDaoJdbc(con).excluirDoUsuario(id);
                new VotoDenunciaDaoJdbc(con).excluirDoUsuario(id);
                new DenunciaDaoJdbc(con).excluirDoAutor(id);
                new PerfilDemograficoDaoJdbc(con).excluir(id);
                new UsuarioDaoJdbc(con).excluir(id);
            }
            new SemeaduraCidadeDaoJdbc(con).excluirTodas();
            return simulados.size();
        });
    }

    /** INSERTs do pacote da cidade, para o script .sql de carga inicial. */
    public String exportarSql(String cidade, String uf) {
        String nomeCidade = TextoUtil.capitalizar(cidade);
        String siglaUf = uf.trim().toUpperCase();
        String chave = TextoUtil.chaveCidade(cidade);
        PacoteCidade pacote = new GeradorConteudo(GeradorConteudo.randomDaCidade(chave, siglaUf), catalogo())
                .gerarPacote(LocalDateTime.now());
        return new ExportadorSql().exportar(pacote, nomeCidade, siglaUf, emailBase(nomeCidade, siglaUf));
    }

    // ================================================================== montagem dos objetos

    private Usuario morador(String nome, int posicao, String cidade, String uf, LocalDateTime cadastro) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(emailBase(cidade, uf).replace("{n}", String.valueOf(posicao + 1)));
        usuario.setSenhaHash(SEM_LOGIN);
        usuario.setCidade(cidade);
        usuario.setUf(uf);
        usuario.setDataCadastro(cadastro);
        usuario.setSimulado(true);
        return usuario;
    }

    /** Ex.: morador.{n}.sao.paulo.sp@simulado.meusdireitos.local (o {n} é trocado pela posição). */
    static String emailBase(String cidade, String uf) {
        String slug = TextoUtil.slug(cidade);
        if (slug.length() > 60) {
            slug = slug.substring(0, 60);
        }
        return "morador.{n}." + slug + "." + uf.toLowerCase() + DOMINIO_EMAIL;
    }

    private static Denuncia denuncia(DenunciaPlanejada p, List<Long> ids, String cidade, String uf) {
        Denuncia d = new Denuncia();
        d.setIdAutor(ids.get(p.autor()));
        d.setTitulo(p.titulo());
        d.setDescricao(p.descricao());
        d.setCategoria(p.categoria());
        d.setStatus(p.status());
        d.setEndereco(p.endereco());
        d.setBairro(p.bairro());
        d.setCidade(cidade);
        d.setUf(uf);
        d.setImagemUrl(p.imagemUrl());
        d.setDataCriacao(p.criadaEm());
        return d;
    }

    private static Comentario comentario(Long idDenuncia, Long idAutor, String texto, LocalDateTime quando) {
        Comentario c = new Comentario();
        c.setIdDenuncia(idDenuncia);
        c.setIdAutor(idAutor);
        c.setTexto(texto);
        c.setDataCriacao(quando);
        return c;
    }

    /** Sorteia um morador simulado que não seja o autor da denúncia. */
    private static Usuario outroMorador(GeradorConteudo gerador, List<Usuario> moradores, Denuncia alvo) {
        Usuario escolhido;
        do {
            escolhido = gerador.escolher(moradores);
        } while (alvo.isAutor(escolhido.getId()));
        return escolhido;
    }
}
