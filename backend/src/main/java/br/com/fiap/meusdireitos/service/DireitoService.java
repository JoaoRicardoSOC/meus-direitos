package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.DireitoDao;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.DireitoDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.DireitoRequest;
import br.com.fiap.meusdireitos.dto.request.PassoRequest;
import br.com.fiap.meusdireitos.dto.response.DireitoResponse;
import br.com.fiap.meusdireitos.exception.ConflitoException;
import br.com.fiap.meusdireitos.model.Direito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras do catálogo de temas de direitos (CRUD).
 */
public class DireitoService {

    static final int MAX_PASSOS = 10;

    /**
     * @param tag filtro opcional recebido na URL (ex.: "IDOSO"); vazio lista todos
     */
    public List<DireitoResponse> listar(String tag) {
        TagDemografica filtro = Validador.converterEnum(TagDemografica.class, tag, "tag");
        return Transacao.consultar(con -> {
            List<DireitoResponse> resposta = new ArrayList<>();
            for (Direito direito : new DireitoDaoJdbc(con).listar(filtro)) {
                resposta.add(DireitoResponse.de(direito));
            }
            return resposta;
        });
    }

    public DireitoResponse buscar(Long id) {
        return Transacao.consultar(con -> DireitoResponse.de(new DireitoDaoJdbc(con).buscarPorId(id)));
    }

    /**
     * Grava o tema, os passos e as tags numa única transação.
     *
     * @throws ConflitoException se já existir um tema com o mesmo título
     */
    public DireitoResponse criar(DireitoRequest req) {
        Direito direito = montar(req);
        return Transacao.executar(con -> {
            DireitoDao dao = new DireitoDaoJdbc(con);
            garantirTituloUnico(dao, direito.getTitulo(), null);
            dao.inserir(direito);
            return DireitoResponse.de(direito);
        });
    }

    /** Substitui todos os dados do tema, inclusive passos e tags. */
    public DireitoResponse atualizar(Long id, DireitoRequest req) {
        Direito direito = montar(req);
        direito.setId(id);
        return Transacao.executar(con -> {
            DireitoDao dao = new DireitoDaoJdbc(con);
            dao.buscarPorId(id); // 404 se não existir
            garantirTituloUnico(dao, direito.getTitulo(), id);
            dao.atualizar(direito);
            return DireitoResponse.de(direito);
        });
    }

    public void excluir(Long id) {
        Transacao.executarSemRetorno(con -> new DireitoDaoJdbc(con).excluir(id));
    }

    private static void garantirTituloUnico(DireitoDao dao, String titulo, Long idAtual) throws java.sql.SQLException {
        Long idExistente = dao.buscarIdPorTitulo(titulo);
        if (idExistente != null && !idExistente.equals(idAtual)) {
            throw new ConflitoException("Já existe um tema com este título.");
        }
    }

    /** Valida o pedido e converte para a entidade. */
    private static Direito montar(DireitoRequest req) {
        Validador.corpoObrigatorio(req);
        Validador v = new Validador()
                .obrigatorio("titulo", req.titulo()).tamanho("titulo", req.titulo(), 3, 120)
                .obrigatorio("resumo", req.resumo()).tamanho("resumo", req.resumo(), 10, 200)
                .obrigatorio("descricao", req.descricao()).tamanho("descricao", req.descricao(), 10, 1000)
                .tamanho("aviso", req.aviso(), 0, 300)
                .tamanho("baseLegal", req.baseLegal(), 0, 300)
                .url("linkOficial", req.linkOficial())
                .intervalo("ordem", req.ordem(), 0, 999);

        List<PassoRequest> passos = req.passos() == null ? List.of() : req.passos();
        v.regra(!passos.isEmpty() && passos.size() <= MAX_PASSOS, "passos",
                "Informe de 1 a " + MAX_PASSOS + " passos.");
        for (int i = 0; i < passos.size(); i++) {
            PassoRequest passo = passos.get(i);
            String campo = "passos[" + i + "]";
            if (passo == null) {
                v.regra(false, campo, "Passo vazio.");
                continue;
            }
            v.obrigatorio(campo + ".titulo", passo.titulo()).tamanho(campo + ".titulo", passo.titulo(), 3, 80)
                    .obrigatorio(campo + ".texto", passo.texto()).tamanho(campo + ".texto", passo.texto(), 5, 400);
        }

        List<TagDemografica> tags = req.tags() == null ? List.of() : req.tags();
        v.regra(!tags.isEmpty() && !tags.contains(null), "tags",
                "Informe pelo menos uma tag válida (use TODOS para qualquer pessoa).");
        v.lancarSeHouverErros();

        Direito direito = new Direito();
        direito.setTitulo(req.titulo());
        direito.setResumo(req.resumo());
        direito.setDescricao(req.descricao());
        direito.setAviso(req.aviso());
        direito.setBaseLegal(req.baseLegal());
        direito.setLinkOficial(req.linkOficial());
        direito.setIcone(req.icone());
        direito.setOrdem(req.ordem() == null ? 0 : req.ordem());
        for (PassoRequest passo : passos) {
            direito.adicionarPasso(passo.titulo(), passo.texto());
        }
        for (TagDemografica tag : tags) {
            direito.adicionarTag(tag);
        }
        return direito;
    }
}
