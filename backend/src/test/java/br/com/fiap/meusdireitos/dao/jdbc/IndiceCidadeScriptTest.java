package br.com.fiap.meusdireitos.dao.jdbc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * O Oracle só usa um índice baseado em função se a expressão da consulta for idêntica à do índice.
 * Este teste confere que os índices de cidade do {@code database/script.sql} usam exatamente a
 * expressão gerada por {@link JdbcUtil#cidadeIgual(String)}.
 */
class IndiceCidadeScriptTest {

    private static final Path SCRIPT = Path.of("..", "database", "script.sql");

    @Test
    void indicesDeCidadeUsamAMesmaExpressaoDaConsulta() throws IOException {
        assumeTrue(Files.exists(SCRIPT), "script.sql não encontrado a partir de " + Path.of("").toAbsolutePath());
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        String condicao = JdbcUtil.cidadeIgual("nm_cidade");
        String expressao = condicao.substring(0, condicao.length() - " = ?".length());

        assertEquals(2, ocorrencias(script, "(" + expressao + ", sg_uf)"),
                "IX_GS26S2_DENUNCIA_CIDADE_CHAVE e IX_GS26S2_USUARIO_CIDADE_CHAVE devem indexar: " + expressao);
    }

    private static int ocorrencias(String texto, String trecho) {
        int total = 0;
        for (int i = texto.indexOf(trecho); i >= 0; i = texto.indexOf(trecho, i + 1)) {
            total++;
        }
        return total;
    }
}
