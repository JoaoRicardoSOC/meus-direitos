package br.com.fiap.meusdireitos.dao.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUtilTest {

    @Test
    void geraMarcadoresDoInDinamico() {
        assertEquals("?", JdbcUtil.placeholders(1));
        assertEquals("?, ?, ?", JdbcUtil.placeholders(3));
    }

    @Test
    void inSemValoresEhErroDeProgramacao() {
        assertThrows(IllegalArgumentException.class, () -> JdbcUtil.placeholders(0));
    }

    @Test
    void comparacaoDeCidadeTiraAcentosComOMesmoTamanho() {
        String sql = JdbcUtil.cidadeIgual("d.nm_cidade");
        assertTrue(sql.startsWith("TRANSLATE(UPPER(REGEXP_REPLACE(TRIM(d.nm_cidade)"));
        assertTrue(sql.endsWith(" = ?"));
        String[] partes = sql.split("'");
        // TRANSLATE troca caractere por caractere: as duas listas precisam ter o mesmo tamanho
        assertEquals(partes[partes.length - 4].length(), partes[partes.length - 2].length());
    }
}
