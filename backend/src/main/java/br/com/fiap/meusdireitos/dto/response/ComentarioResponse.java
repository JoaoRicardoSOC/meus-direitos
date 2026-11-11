package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.Comentario;

import java.time.LocalDateTime;

/**
 * Comentário da "Conversa do bairro".
 *
 * @param meu true se quem está vendo escreveu o comentário (o front mostra editar/excluir)
 */
public record ComentarioResponse(Long id, Long idDenuncia, String texto, AutorResponse autor, boolean meu,
                                 LocalDateTime criadoEm, LocalDateTime editadoEm) {

    public static ComentarioResponse de(Comentario c, Long idLeitor) {
        return new ComentarioResponse(c.getId(), c.getIdDenuncia(), c.getTexto(),
                new AutorResponse(c.getIdAutor(), c.getNomeAutor()), c.isAutor(idLeitor),
                c.getDataCriacao(), c.getDataEdicao());
    }
}
