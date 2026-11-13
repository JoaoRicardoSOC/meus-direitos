package br.com.fiap.meusdireitos.factory;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do pool com uma {@link Connection} falsa: nenhum banco é usado.
 * <p>
 * A conexão falsa é criada com {@link Proxy} só aqui no teste, porque a interface {@code Connection}
 * tem dezenas de métodos e o pool usa poucos. O código de produção não usa proxy.
 */
class PoolConexoesTest {

    /** Estado de uma conexão física falsa, para conferir o que o pool fez com ela. */
    static final class ConexaoFalsa {
        boolean autoCommit = true;
        boolean readOnly;
        boolean fechada;
        boolean valida = true;
        int rollbacks;
        int commits;
        int fechamentos;
        Connection conexao;
    }

    /** Abre conexões falsas e guarda todas para inspeção. */
    static final class AbridorFalso implements PoolConexoes.AbridorConexao {
        final List<ConexaoFalsa> abertas = new ArrayList<>();

        @Override
        public Connection abrir() {
            ConexaoFalsa estado = new ConexaoFalsa();
            estado.conexao = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, metodo, args) -> {
                        switch (metodo.getName()) {
                            case "getAutoCommit": return estado.autoCommit;
                            case "setAutoCommit": estado.autoCommit = (Boolean) args[0]; return null;
                            case "isReadOnly": return estado.readOnly;
                            case "setReadOnly": estado.readOnly = (Boolean) args[0]; return null;
                            case "rollback": estado.rollbacks++; return null;
                            case "commit": estado.commits++; return null;
                            case "isClosed": return estado.fechada;
                            case "isValid": return estado.valida && !estado.fechada;
                            case "close": estado.fechada = true; estado.fechamentos++; return null;
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            case "toString": return "ConexaoFalsa";
                            default: throw new SQLException("não usado no teste: " + metodo.getName());
                        }
                    });
            abertas.add(estado);
            return estado.conexao;
        }
    }

    private final AbridorFalso abridor = new AbridorFalso();

    private PoolConexoes pool(int tamanho, long esperaMs) {
        return new PoolConexoes(tamanho, abridor, esperaMs);
    }

    @Test
    void reaproveitaAConexaoDevolvida() throws SQLException {
        PoolConexoes pool = pool(2, 100);
        Connection primeira = pool.emprestar();
        primeira.setAutoCommit(false);
        primeira.commit();
        pool.devolver(primeira);

        Connection segunda = pool.emprestar();
        assertSame(primeira, segunda, "a segunda requisição usa a mesma conexão física");
        assertTrue(segunda.getAutoCommit());
        pool.devolver(segunda);

        assertEquals(1, abridor.abertas.size());
        assertEquals(1, pool.quantidadeLivres());
        assertEquals(2, pool.vagasDisponiveis());
    }

    @Test
    void devolverFazRollbackERestauraAutoCommitEReadOnly() throws SQLException {
        PoolConexoes pool = pool(1, 100);
        Connection c = pool.emprestar();
        c.setAutoCommit(false);
        c.setReadOnly(true);
        pool.devolver(c);

        ConexaoFalsa fisica = abridor.abertas.get(0);
        assertEquals(1, fisica.rollbacks, "o que ficou sem COMMIT é desfeito");
        assertTrue(fisica.autoCommit);
        assertFalse(fisica.readOnly);
        assertFalse(fisica.fechada, "a conexão continua aberta, na fila do pool");
    }

    @Test
    void emAutoCommitNaoFazRollback() throws SQLException {
        PoolConexoes pool = pool(1, 100);
        pool.devolver(pool.emprestar());
        assertEquals(0, abridor.abertas.get(0).rollbacks);
    }

    @Test
    void devolverDuasVezesContaUmaVezSo() throws SQLException {
        PoolConexoes pool = pool(2, 100);
        Connection c = pool.emprestar();
        pool.devolver(c);
        pool.devolver(c);
        assertEquals(1, pool.quantidadeLivres());
        assertEquals(2, pool.vagasDisponiveis(), "não libera vaga a mais");
    }

    @Test
    void devolverNullOuConexaoDeForaNaoFazNada() throws SQLException {
        PoolConexoes pool = pool(1, 100);
        pool.devolver(null);
        pool.devolver(new AbridorFalso().abrir());
        assertEquals(0, pool.quantidadeLivres());
        assertEquals(1, pool.vagasDisponiveis());
    }

    @Test
    void descartaConexaoInvalidaEAbreOutra() throws SQLException {
        PoolConexoes pool = pool(1, 100);
        pool.devolver(pool.emprestar());
        abridor.abertas.get(0).valida = false; // o banco derrubou a sessão enquanto estava parada

        Connection c = pool.emprestar();
        assertFalse(c.isClosed());
        pool.devolver(c);
        assertEquals(2, abridor.abertas.size());
        assertTrue(abridor.abertas.get(0).fechada, "a conexão inválida é fechada");
        assertFalse(abridor.abertas.get(1).fechada);
    }

    @Test
    void conexaoQueCaiuDuranteOUsoEhDescartadaNaDevolucao() throws SQLException {
        PoolConexoes pool = pool(1, 100);
        Connection c = pool.emprestar();
        abridor.abertas.get(0).fechada = true; // caiu durante o uso
        pool.devolver(c);
        assertEquals(0, pool.quantidadeLivres());
        assertEquals(1, pool.vagasDisponiveis(), "a vaga é liberada mesmo descartando a conexão");
    }

    @Test
    void nuncaPassaDoTamanhoMaximoEEsperaAntesDeDesistir() throws SQLException {
        PoolConexoes pool = pool(2, 150);
        Connection a = pool.emprestar();
        Connection b = pool.emprestar();

        long inicio = System.nanoTime();
        SQLException erro = assertThrows(SQLException.class, pool::emprestar);
        long esperouMs = (System.nanoTime() - inicio) / 1_000_000;

        assertEquals("08001", erro.getSQLState(), "classe 08: a API responde 503");
        assertTrue(esperouMs >= 100, "espera antes de desistir (esperou " + esperouMs + " ms)");
        assertEquals(2, abridor.abertas.size());

        pool.devolver(a);
        Connection c = pool.emprestar();
        assertSame(a, c, "reaproveitou a conexão devolvida");
        pool.devolver(c);
        pool.devolver(b);
        assertEquals(2, abridor.abertas.size());
    }

    @Test
    void quemEstaEsperandoRecebeAConexaoDevolvida() throws Exception {
        PoolConexoes pool = pool(1, 5_000);
        Connection ocupada = pool.emprestar();
        Connection[] recebida = new Connection[1];
        Thread outra = new Thread(() -> {
            try {
                recebida[0] = pool.emprestar();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        outra.start();
        Thread.sleep(50);
        pool.devolver(ocupada);
        outra.join(2_000);

        assertFalse(outra.isAlive());
        assertSame(ocupada, recebida[0]);
        assertEquals(1, abridor.abertas.size());
        pool.devolver(recebida[0]);
    }

    @Test
    void falhaAoAbrirLiberaAVaga() {
        PoolConexoes pool = new PoolConexoes(1, () -> {
            throw new SQLException("banco fora do ar", "08006");
        }, 100);
        assertThrows(SQLException.class, pool::emprestar);
        assertEquals(1, pool.vagasDisponiveis());
    }

    @Test
    void fecharLivresFechaAsConexoes() throws SQLException {
        PoolConexoes pool = pool(2, 100);
        Connection a = pool.emprestar();
        Connection b = pool.emprestar();
        pool.devolver(a);
        pool.devolver(b);
        pool.fecharLivres();
        assertEquals(0, pool.quantidadeLivres());
        assertTrue(abridor.abertas.stream().allMatch(c -> c.fechada));
    }

    @Test
    void tamanhoDoPoolVemDaVariavelComPadraoELimites() {
        assertEquals(5, ConnectionFactory.tamanhoDoPool(null));
        assertEquals(5, ConnectionFactory.tamanhoDoPool(" "));
        assertEquals(5, ConnectionFactory.tamanhoDoPool("abc"));
        assertEquals(8, ConnectionFactory.tamanhoDoPool(" 8 "));
        assertEquals(1, ConnectionFactory.tamanhoDoPool("0"));
        assertEquals(50, ConnectionFactory.tamanhoDoPool("999"));
    }
}
