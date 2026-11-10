package br.com.fiap.meusdireitos.exception.mapper;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Última linha de defesa: qualquer erro não previsto vira um 500 com mensagem genérica.
 * O stack trace vai apenas para o log, nunca para o cliente.
 */
@Provider
public class ErroInesperadoMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErroInesperadoMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable e) {
        LOG.log(Level.SEVERE, "Erro inesperado ao processar a requisição", e);
        return RespostaErro.montar(500, "Erro interno", "Erro inesperado. Tente novamente.", List.of(), uriInfo);
    }
}
