package br.com.fiap.meusdireitos.dao.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
