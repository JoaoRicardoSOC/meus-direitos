package br.com.fiap.meusdireitos.config;

import br.com.fiap.meusdireitos.dto.response.ErroResponse;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respostas que o Grizzly daria por conta própria, fora do Jersey: um caminho que não começa com /api
 * (ex.: /api/../etc/passwd vira /etc/passwd) ou um erro antes de a requisição chegar à API.
 * <p>
 * Sem isto, o Grizzly devolve uma página HTML com o nome e a versão do servidor, o que expõe um detalhe
 * técnico e quebra o contrato de "todo erro é o mesmo JSON". Aqui a resposta segue o {@link ErroResponse}.
 */
public final class RespostaForaDaApi extends HttpHandler implements ErrorPageGenerator {

    private static final Jsonb JSONB = JsonbBuilder.create();

    /** Qualquer caminho fora de /api: 404 no formato padrão. */
    @Override
    public void service(Request request, Response response) throws Exception {
        String corpo = json(404, "Recurso não encontrado", "A rota solicitada não existe.", request.getRequestURI());
        response.setStatus(404);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    /** Página de erro gerada pelo próprio Grizzly (ex.: 404 de contexto, 500 antes do Jersey). */
    @Override
    public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
        if (request != null && request.getResponse() != null) {
            request.getResponse().setContentType("application/json");
        }
        String caminho = request != null ? request.getRequestURI() : null;
        String titulo = status == 404 ? "Recurso não encontrado" : status >= 500 ? "Erro interno" : "Requisição inválida";
        String mensagem = status == 404 ? "A rota solicitada não existe."
                : status >= 500 ? "Erro inesperado. Tente novamente." : "A requisição não pôde ser processada.";
        return json(status, titulo, mensagem, caminho);
    }

    private static String json(int status, String erro, String mensagem, String caminho) {
        return JSONB.toJson(new ErroResponse(status, erro, mensagem, List.of(), caminho, LocalDateTime.now()));
    }
}
