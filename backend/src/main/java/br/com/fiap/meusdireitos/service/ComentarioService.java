package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.ComentarioDao;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.ComentarioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.ComentarioRequest;
import br.com.fiap.meusdireitos.dto.response.ComentarioResponse;
import br.com.fiap.meusdireitos.exception.AcessoNegadoException;
import br.com.fiap.meusdireitos.model.Comentario;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.util.ArrayList;
import java.util.List;

/**
 * "Conversa do bairro": comentários nas denúncias. Só o autor edita ou exclui o próprio comentário.
 */
public class ComentarioService {

    public List<ComentarioResponse> listar(Long idDenuncia, Long idLeitor) {
        return Transacao.consultar(con -> {
            new DenunciaDaoJdbc(con).buscarPorId(idDenuncia); // 404 se a denúncia não existir
            List<ComentarioResponse> resposta = new ArrayList<>();
            for (Comentario c : new ComentarioDaoJdbc(con).listarPorDenuncia(idDenuncia)) {
                resposta.add(ComentarioResponse.de(c, idLeitor));
            }
            return resposta;
        });
    }

    public ComentarioResponse criar(Long idUsuario, Long idDenuncia, ComentarioRequest req) {
        validar(req);
        return Transacao.executar(con -> {
            new DenunciaDaoJdbc(con).buscarPorId(idDenuncia); // 404 se a denúncia não existir
            Comentario comentario = new Comentario();
            comentario.setIdDenuncia(idDenuncia);
            comentario.setIdAutor(idUsuario);
            comentario.setTexto(req.texto());
            ComentarioDao dao = new ComentarioDaoJdbc(con);
            dao.inserir(comentario);
            return ComentarioResponse.de(dao.buscarPorId(comentario.getId()), idUsuario);
        });
    }

    public ComentarioResponse editar(Long idUsuario, Long idComentario, ComentarioRequest req) {
        validar(req);
        return Transacao.executar(con -> {
            ComentarioDao dao = new ComentarioDaoJdbc(con);
            Comentario comentario = dao.buscarPorId(idComentario);
            exigirAutor(comentario, idUsuario);
            comentario.setTexto(req.texto());
            dao.atualizar(comentario);
            return ComentarioResponse.de(comentario, idUsuario);
        });
    }

    public void excluir(Long idUsuario, Long idComentario) {
        Transacao.executarSemRetorno(con -> {
            ComentarioDao dao = new ComentarioDaoJdbc(con);
            exigirAutor(dao.buscarPorId(idComentario), idUsuario);
            dao.excluir(idComentario);
        });
    }

    private static void exigirAutor(Comentario comentario, Long idUsuario) {
        if (!comentario.isAutor(idUsuario)) {
            throw new AcessoNegadoException("Somente quem escreveu o comentário pode alterá-lo.");
        }
    }

    private static void validar(ComentarioRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador()
                .obrigatorio("texto", req.texto()).tamanho("texto", req.texto(), 2, 500)
                .lancarSeHouverErros();
    }
}
