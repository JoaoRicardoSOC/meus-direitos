package br.com.fiap.meusdireitos.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenciaExceptionTest {

    private static SQLException ora(int codigo) {
        return new SQLException("ORA-" + codigo + ": mensagem técnica", "23000", codigo);
    }

    @Test
    void uniqueVioladaViraConflito409() {
        MeusDireitosException e = PersistenciaException.de(ora(1));
        assertInstanceOf(ConflitoException.class, e);
        assertEquals(409, e.getStatusHttp());
    }

    @Test
    void chaveEstrangeiraInexistenteVira404() {
        assertEquals(404, PersistenciaException.de(ora(2291)).getStatusHttp());
    }

    @Test
    void registroComFilhosVira409() {
        assertEquals(409, PersistenciaException.de(ora(2292)).getStatusHttp());
    }

    @Test
    void checkNotNullETamanhoViramValidacao400() {
        assertInstanceOf(ValidacaoException.class, PersistenciaException.de(ora(2290)));
        assertInstanceOf(ValidacaoException.class, PersistenciaException.de(ora(1400)));
        assertInstanceOf(ValidacaoException.class, PersistenciaException.de(ora(12899)));
    }

    @Test
    void falhaDeConexaoVira503() {
        MeusDireitosException porCodigo = PersistenciaException.de(new SQLException("ORA-01017", "72000", 1017));
        MeusDireitosException porSqlState = PersistenciaException.de(new SQLException("rede", "08006", 0));
        MeusDireitosException recuperavel = PersistenciaException.de(new SQLRecoverableException("IO Error"));

        assertEquals(503, porCodigo.getStatusHttp());
        assertEquals(503, porSqlState.getStatusHttp());
        assertEquals(503, recuperavel.getStatusHttp());
        assertTrue(((PersistenciaException) porCodigo).isBancoIndisponivel());
    }

    @Test
    void erroDesconhecidoVira500SemExporMensagemTecnica() {
        SQLException original = new SQLException("ORA-00942: table or view does not exist", "42000", 942);
        MeusDireitosException e = PersistenciaException.de(original);

        assertEquals(500, e.getStatusHttp());
        assertFalse(e.getMessage().contains("ORA-"));
        assertSame(original, e.getCause()); // detalhe técnico preservado para o log
    }
}
