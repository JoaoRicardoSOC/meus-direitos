package br.com.fiap.meusdireitos.exception.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Converte os erros gerados pelo próprio Jersey (rota inexistente, método não permitido,
 * corpo vazio, Content-Type errado...) no JSON de erro padrão, preservando o status original.
 * <p>
 * Sem este mapper, o {@link ErroInesperadoMapper} (que captura {@code Throwable}) transformaria
 * um simples 404 de rota em 500.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        Response resposta = RespostaErro.montar(status, titulo(status), mensagem(status), List.of(), uriInfo);

        // Mantém o cabeçalho Allow (lista os métodos aceitos) nas respostas 405
        Object permitido = e.getResponse().getHeaders().getFirst(HttpHeaders.ALLOW);
        if (permitido != null) {
            return Response.fromResponse(resposta).header(HttpHeaders.ALLOW, permitido).build();
        }
        return resposta;
    }

    private static String titulo(int status) {
        switch (status) {
            case 400: return "Requisição inválida";
            case 401: return "Não autenticado";
            case 403: return "Acesso negado";
            case 404: return "Recurso não encontrado";
            case 405: return "Método não permitido";
            case 406: return "Formato não suportado";
            case 415: return "Tipo de conteúdo não suportado";
            default:  return status >= 500 ? "Erro interno" : "Requisição inválida";
        }
    }

    private static String mensagem(int status) {
        switch (status) {
            case 400: return "A requisição está incompleta ou mal formatada. Verifique o corpo enviado.";
            case 404: return "A rota solicitada não existe.";
            case 405: return "Este método HTTP não é aceito nesta rota.";
            case 406: return "A API responde somente em JSON (application/json).";
            case 415: return "Envie o corpo em JSON, com o cabeçalho Content-Type: application/json.";
            default:  return status >= 500 ? "Erro inesperado. Tente novamente." : "Não foi possível processar a requisição.";
        }
    }
}
