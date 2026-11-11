package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.DenunciaDao;
import br.com.fiap.meusdireitos.dao.FiltroDenuncia;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.ComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.ConfirmacaoResolucaoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.VotoDenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.DenunciaRequest;
import br.com.fiap.meusdireitos.dto.request.StatusRequest;
import br.com.fiap.meusdireitos.dto.response.DenunciaResponse;
import br.com.fiap.meusdireitos.dto.response.PaginaResponse;
import br.com.fiap.meusdireitos.exception.AcessoNegadoException;
import br.com.fiap.meusdireitos.exception.RegraNegocioException;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.DenunciaDetalhada;
import br.com.fiap.meusdireitos.model.Pagina;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras das denúncias: registro, consulta, edição, mudança de status e exclusão.
 * Apenas o autor pode alterar ou excluir a própria denúncia.
 */
public class DenunciaService {

    static final int TAMANHO_PADRAO = 10;
    static final int TAMANHO_MAXIMO = 50;
    static final int PAGINA_MAXIMA = 10_000;
    static final int TAMANHO_MAXIMO_BUSCA = 100;

    /**
     * Registra uma denúncia na cidade do autor.
     *
     * @return a denúncia criada, com o protocolo (tela "Sua voz foi ouvida")
     */
    public DenunciaResponse criar(Long idUsuario, DenunciaRequest req) {
        validar(req);
        return Transacao.executar(con -> {
            Usuario autor = new UsuarioDaoJdbc(con).buscarPorId(idUsuario);
            Denuncia denuncia = new Denuncia();
            denuncia.setIdAutor(idUsuario);
            preencher(denuncia, req);
            denuncia.setCidade(autor.getCidade());
            denuncia.setUf(autor.getUf());
            DenunciaDao dao = new DenunciaDaoJdbc(con);
            dao.inserir(denuncia);
            return DenunciaResponse.de(dao.buscarDetalhada(denuncia.getId(), idUsuario));
        });
    }

    public DenunciaResponse buscar(Long id, Long idLeitor) {
        return Transacao.consultar(con -> DenunciaResponse.de(new DenunciaDaoJdbc(con).buscarDetalhada(id, idLeitor)));
    }

    /**
     * Listagem pública com filtros opcionais (texto recebido na URL), da mais recente para a mais antiga.
     */
    public PaginaResponse<DenunciaResponse> pesquisar(String cidade, String uf, String categoria, String status,
                                                      String busca, String pagina, String tamanho, Long idLeitor) {
        new Validador().tamanho("busca", busca, 0, TAMANHO_MAXIMO_BUSCA).lancarSeHouverErros();
        FiltroDenuncia filtro = new FiltroDenuncia(vazioParaNulo(cidade), vazioParaNulo(uf),
                Validador.converterEnum(CategoriaDenuncia.class, categoria, "categoria"),
                Validador.converterEnum(StatusDenuncia.class, status, "status"),
                vazioParaNulo(busca), null);
        int numeroPagina = Validador.inteiro("pagina", pagina, 1, 1, PAGINA_MAXIMA);
        int tamanhoPagina = Validador.inteiro("tamanho", tamanho, TAMANHO_PADRAO, 1, TAMANHO_MAXIMO);
        return Transacao.consultar(con ->
                paraResposta(new DenunciaDaoJdbc(con).pesquisar(filtro, idLeitor, numeroPagina, tamanhoPagina)));
    }

    /** Denúncias do próprio usuário (tela "Minha denúncia"). */
    public List<DenunciaResponse> minhas(Long idUsuario) {
        return Transacao.consultar(con -> paraResposta(new DenunciaDaoJdbc(con).listarPorAutor(idUsuario, idUsuario)));
    }

    /**
     * Edita título, descrição, categoria, endereço, bairro e imagem.
     *
     * @throws AcessoNegadoException se quem pede não for o autor
     */
    public DenunciaResponse atualizar(Long idUsuario, Long id, DenunciaRequest req) {
        validar(req);
        return Transacao.executar(con -> {
            DenunciaDao dao = new DenunciaDaoJdbc(con);
            Denuncia denuncia = dao.buscarPorId(id);
            exigirAutor(denuncia, idUsuario, "editá-la");
            preencher(denuncia, req);
            dao.atualizar(denuncia);
            return DenunciaResponse.de(dao.buscarDetalhada(id, idUsuario));
        });
    }

    /**
     * @throws RegraNegocioException se a transição não for permitida (ex.: RESOLVIDA → EM_ANDAMENTO)
     */
    public DenunciaResponse alterarStatus(Long idUsuario, Long id, StatusRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador().obrigatorio("status", req.status()).lancarSeHouverErros();
        return Transacao.executar(con -> {
            DenunciaDao dao = new DenunciaDaoJdbc(con);
            Denuncia denuncia = dao.buscarPorId(id);
            exigirAutor(denuncia, idUsuario, "alterar o status dela");
            if (!denuncia.getStatus().podeIrPara(req.status())) {
                throw new RegraNegocioException("Não é possível mudar de '" + denuncia.getStatus().getDescricao()
                        + "' para '" + req.status().getDescricao() + "'.");
            }
            dao.atualizarStatus(id, req.status());
            if (req.status() == StatusDenuncia.ABERTA) {
                // reaberta: as confirmações antigas não valem mais (o problema voltou)
                new ConfirmacaoResolucaoDaoJdbc(con).excluirPorDenuncia(id);
            }
            return DenunciaResponse.de(dao.buscarDetalhada(id, idUsuario));
        });
    }

    /** Exclui a denúncia com seus comentários, votos e confirmações, numa única transação. */
    public void excluir(Long idUsuario, Long id) {
        Transacao.executarSemRetorno(con -> {
            DenunciaDao dao = new DenunciaDaoJdbc(con);
            exigirAutor(dao.buscarPorId(id), idUsuario, "excluí-la");
            new ConfirmacaoResolucaoDaoJdbc(con).excluirPorDenuncia(id);
            new ComentarioDaoJdbc(con).excluirPorDenuncia(id);
            new VotoDenunciaDaoJdbc(con).excluirPorDenuncia(id);
            dao.excluir(id);
        });
    }

    // ------------------------------------------------------------------ auxiliares

    static PaginaResponse<DenunciaResponse> paraResposta(Pagina<DenunciaDetalhada> pagina) {
        return new PaginaResponse<>(pagina.pagina(), pagina.tamanho(), pagina.total(), pagina.temProxima(),
                paraResposta(pagina.itens()));
    }

    static List<DenunciaResponse> paraResposta(List<DenunciaDetalhada> itens) {
        List<DenunciaResponse> resposta = new ArrayList<>();
        for (DenunciaDetalhada item : itens) {
            resposta.add(DenunciaResponse.de(item));
        }
        return resposta;
    }

    private static void exigirAutor(Denuncia denuncia, Long idUsuario, String acao) {
        if (!denuncia.isAutor(idUsuario)) {
            throw new AcessoNegadoException("Somente quem registrou a denúncia pode " + acao + ".");
        }
    }

    private static void validar(DenunciaRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador()
                .obrigatorio("titulo", req.titulo()).tamanho("titulo", req.titulo(), 5, 120)
                .obrigatorio("descricao", req.descricao()).tamanho("descricao", req.descricao(), 10, 1000)
                .obrigatorio("categoria", req.categoria())
                .obrigatorio("endereco", req.endereco()).tamanho("endereco", req.endereco(), 5, 150)
                .tamanho("bairro", req.bairro(), 0, 80)
                .imagem("imagemUrl", req.imagemUrl())
                .lancarSeHouverErros();
    }

    private static void preencher(Denuncia denuncia, DenunciaRequest req) {
        denuncia.setTitulo(req.titulo());
        denuncia.setDescricao(req.descricao());
        denuncia.setCategoria(req.categoria());
        denuncia.setEndereco(req.endereco());
        denuncia.setBairro(req.bairro());
        denuncia.setImagemUrl(req.imagemUrl());
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
