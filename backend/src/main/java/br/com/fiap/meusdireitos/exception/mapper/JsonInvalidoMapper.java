package br.com.fiap.meusdireitos.exception.mapper;

import br.com.fiap.meusdireitos.exception.ErroCampo;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trata o corpo JSON que não pôde ser convertido para o DTO: JSON mal formado, valor de enum
 * inexistente (ex.: categoria "XYZ"), data fora do formato AAAA-MM-DD, texto em campo numérico.
 * <p>
 * O JSON-B do Jersey embrulha esses erros em {@link ProcessingException}. Sem este mapper,
 * o cliente recebia uma página HTML de erro 500.
 */
@Provider
public class JsonInvalidoMapper implements ExceptionMapper<ProcessingException> {

    private static final Logger LOG = Logger.getLogger(JsonInvalidoMapper.class.getName());

    /** O Yasson (JSON-B) informa o campo com problema no formato: property 'nome'. */
    private static final Pattern CAMPO = Pattern.compile("property '([A-Za-z0-9_]+)'");

    /** Valor de enum inexistente: "No enum constant pacote.Tipo.VALOR". */
    private static final Pattern ENUM_INVALIDO = Pattern.compile("No enum constant ([A-Za-z0-9_.$]+)\\.([A-Za-z0-9_]+)");

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ProcessingException e) {
        JsonbException erroJson = encontrarErroJson(e);
        if (erroJson == null) {
            // ProcessingException sem relação com o JSON de entrada: é um erro interno
            LOG.log(Level.SEVERE, "Erro de processamento inesperado", e);
            return RespostaErro.montar(500, "Erro interno", "Erro inesperado. Tente novamente.", List.of(), uriInfo);
        }

        String detalhes = mensagensDaCadeia(e);

        Matcher enumInvalido = ENUM_INVALIDO.matcher(detalhes);
        if (enumInvalido.find()) {
            String mensagem = "Valor '" + enumInvalido.group(2) + "' não é aceito."
                    + valoresAceitos(enumInvalido.group(1));
            return RespostaErro.montar(400, "JSON inválido", mensagem, List.of(), uriInfo);
        }

        List<ErroCampo> campos = List.of();
        Matcher campo = CAMPO.matcher(detalhes);
        if (campo.find()) {
            campos = List.of(new ErroCampo(campo.group(1), "Valor em formato inválido."));
        }
        return RespostaErro.montar(400, "JSON inválido",
                "O corpo da requisição não está no formato esperado. Verifique o JSON, os valores das listas e as datas (AAAA-MM-DD).",
                campos, uriInfo);
    }

    /**
     * Monta o texto " Valores aceitos: A, B, C." a partir do nome do enum citado pelo JSON-B.
     *
     * @param nomeCanonico nome do enum como aparece na mensagem (ex.: br.com...CategoriaDenuncia)
     * @return lista de valores, ou texto vazio se o enum não puder ser identificado
     */
    private static String valoresAceitos(String nomeCanonico) {
        Class<?> tipo = carregarClasse(nomeCanonico);
        if (tipo == null || !tipo.isEnum()) {
            return "";
        }
        StringJoiner valores = new StringJoiner(", ", " Valores aceitos: ", ".");
        for (Object constante : tipo.getEnumConstants()) {
            valores.add(((Enum<?>) constante).name());
        }
        return valores.toString();
    }

    private static Class<?> carregarClasse(String nomeCanonico) {
        String nome = nomeCanonico;
        while (true) {
            try {
                return Class.forName(nome);
            } catch (ClassNotFoundException e) {
                int ultimoPonto = nome.lastIndexOf('.');
                if (ultimoPonto < 0) {
                    return null;
                }
                // enum aninhado: Externa.Interna -> Externa$Interna
                nome = nome.substring(0, ultimoPonto) + '$' + nome.substring(ultimoPonto + 1);
                if (nome.indexOf('.') < 0) {
                    return null;
                }
            }
        }
    }

    private static JsonbException encontrarErroJson(Throwable e) {
        for (Throwable atual = e; atual != null; atual = atual.getCause()) {
            if (atual instanceof JsonbException) {
                return (JsonbException) atual;
            }
        }
        return null;
    }

    private static String mensagensDaCadeia(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable atual = e; atual != null; atual = atual.getCause()) {
            sb.append(atual.getMessage()).append(' ');
        }
        return sb.toString();
    }
}
