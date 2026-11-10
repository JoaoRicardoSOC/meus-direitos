package br.com.fiap.meusdireitos.exception.mapper;

import br.com.fiap.meusdireitos.exception.MeusDireitosException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Converte qualquer exceção da aplicação no JSON de erro padrão.
 * O status vem da própria exceção (polimorfismo), sem if/else por tipo.
 */
@Provider
public class MeusDireitosExceptionMapper implements ExceptionMapper<MeusDireitosException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(MeusDireitosException e) {
        return RespostaErro.montar(e.getStatusHttp(), e.getTitulo(), e.getMessage(), e.getCampos(), uriInfo);
    }
}
