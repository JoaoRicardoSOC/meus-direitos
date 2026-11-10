package br.com.fiap.meusdireitos.exception.mapper;

import br.com.fiap.meusdireitos.dto.response.ErroResponse;
import br.com.fiap.meusdireitos.exception.ErroCampo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Monta a {@link Response} HTTP com o {@link ErroResponse} padrão. Usado por todos os mappers.
 */
final class RespostaErro {

    private RespostaErro() {
    }

    static Response montar(int status, String erro, String mensagem, List<ErroCampo> campos, UriInfo uriInfo) {
        ErroResponse corpo = new ErroResponse(status, erro, mensagem, campos, caminho(uriInfo), LocalDateTime.now());
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"))
                .entity(corpo)
                .build();
    }

    private static String caminho(UriInfo uriInfo) {
        try {
            return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
        } catch (RuntimeException e) {
            return null; // fora do contexto de uma requisição
        }
    }
}
