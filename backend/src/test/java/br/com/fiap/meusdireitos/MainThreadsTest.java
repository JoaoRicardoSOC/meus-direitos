package br.com.fiap.meusdireitos;

import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O número de threads que atendem a API não pode depender dos processadores da máquina:
 * no Render gratuito a JVM enxerga 1 processador, e o padrão do Grizzly (processadores × 2) dava 2 threads.
 */
class MainThreadsTest {

    @Test
    void threadsDeTrabalhoSaoFixasEMaioresQueOPoolDeConexoes() {
        ThreadPoolConfig config = Main.configuracaoDasThreads();

        assertEquals(Main.THREADS_DE_TRABALHO, config.getCorePoolSize());
        assertEquals(Main.THREADS_DE_TRABALHO, config.getMaxPoolSize());
        // sobram threads para o que não usa o banco (health check, listas fixas) com o pool de 5 ocupado
        assertTrue(Main.THREADS_DE_TRABALHO > 5);
    }
}
