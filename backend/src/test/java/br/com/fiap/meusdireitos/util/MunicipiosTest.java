package br.com.fiap.meusdireitos.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MunicipiosTest {

    @Test
    void listaTemTodosOsMunicipiosEUfsDoIbge() {
        assertEquals(27, Municipios.quantidadeDeUfs());
        assertEquals(5571, Municipios.total());
    }

    @Test
    void devolveAGrafiaOficialIgnorandoAcentosMaiusculasEEspacos() {
        assertEquals(Optional.of("Jundiaí"), Municipios.nomeOficial("jundiai", "SP"));
        assertEquals(Optional.of("Jundiaí"), Municipios.nomeOficial("  JUNDIAÍ ", "sp"));
        assertEquals(Optional.of("São José dos Campos"), Municipios.nomeOficial("sao   jose dos campos", "SP"));
        assertEquals(Optional.of("Brasília"), Municipios.nomeOficial("Brasilia", "DF"));
        assertEquals(Optional.of("Pau D'Arco"), Municipios.nomeOficial("pau d'arco", "TO"));
    }

    @Test
    void cidadeDeOutroEstadoOuInexistenteNaoEhAceita() {
        assertTrue(Municipios.nomeOficial("Campinas", "RJ").isEmpty());
        assertTrue(Municipios.nomeOficial("Jundia�", "SP").isEmpty()); // o nome corrompido do teste com curl
        assertTrue(Municipios.nomeOficial("Cidade Inventada", "SP").isEmpty());
        assertTrue(Municipios.nomeOficial("Campinas", "ZZ").isEmpty());
        assertTrue(Municipios.nomeOficial(null, "SP").isEmpty());
        assertTrue(Municipios.nomeOficial("Campinas", null).isEmpty());
    }

    @Test
    void listaDaApiEhIgualAListaDoApp() throws IOException {
        // os testes rodam na pasta backend/; fora do monorepo (ex.: imagem Docker) o arquivo do front não existe
        Path doApp = Path.of("..", "frontend", "src", "data", "municipios.json");
        assumeTrue(Files.exists(doApp), "lista do front não encontrada");
        byte[] daApi;
        try (var entrada = Municipios.class.getResourceAsStream(Municipios.ARQUIVO)) {
            daApi = entrada.readAllBytes();
        }
        assertArrayEquals(Files.readAllBytes(doApp), daApi,
                "backend/src/main/resources/municipios.json e frontend/src/data/municipios.json precisam ser iguais");
    }
}
