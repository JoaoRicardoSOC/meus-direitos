package br.com.fiap.meusdireitos.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lista oficial de municípios do IBGE por UF ({@code municipios.json}, a mesma do front em
 * {@code frontend/src/data/municipios.json}), carregada uma vez na primeira consulta.
 * <p>
 * A cidade do cadastro precisa ser um município do estado escolhido, e é gravada com a grafia oficial
 * ("jundiai", "JUNDIAÍ" e "Jundiaí" viram "Jundiaí"). Assim a mesma cidade não aparece escrita de vários
 * jeitos no banco. A comparação ignora acentos, maiúsculas e espaços repetidos, como {@link TextoUtil#chaveCidade}.
 */
public final class Municipios {

    static final String ARQUIVO = "/municipios.json";

    private Municipios() {
    }

    /**
     * @return o nome oficial do município, se {@code cidade} existir na {@code uf}; vazio caso contrário
     * (inclusive para valores nulos ou UF inexistente)
     */
    public static Optional<String> nomeOficial(String cidade, String uf) {
        if (cidade == null || uf == null) {
            return Optional.empty();
        }
        Map<String, String> doEstado = Lista.POR_UF.get(uf.trim().toUpperCase());
        return doEstado == null ? Optional.empty() : Optional.ofNullable(doEstado.get(TextoUtil.chaveCidade(cidade)));
    }

    /** @return quantos municípios há na lista (todas as UFs) */
    public static int total() {
        return Lista.POR_UF.values().stream().mapToInt(Map::size).sum();
    }

    /** @return quantas UFs há na lista */
    public static int quantidadeDeUfs() {
        return Lista.POR_UF.size();
    }

    /** Carregada só quando alguém consulta (padrão "holder": a JVM garante uma carga só, sem sincronizar). */
    private static final class Lista {
        static final Map<String, Map<String, String>> POR_UF = carregar();

        private static Map<String, Map<String, String>> carregar() {
            Type tipo = new HashMap<String, List<String>>() { }.getClass().getGenericSuperclass();
            try (InputStream entrada = Municipios.class.getResourceAsStream(ARQUIVO); Jsonb jsonb = JsonbBuilder.create()) {
                if (entrada == null) {
                    throw new IllegalStateException(ARQUIVO + " não encontrado no classpath");
                }
                Map<String, List<String>> nomes = jsonb.fromJson(entrada, tipo);
                Map<String, Map<String, String>> porUf = new HashMap<>();
                nomes.forEach((uf, cidades) -> {
                    Map<String, String> porChave = new HashMap<>();
                    cidades.forEach(nome -> porChave.put(TextoUtil.chaveCidade(nome), nome));
                    porUf.put(uf, Collections.unmodifiableMap(porChave));
                });
                return Collections.unmodifiableMap(porUf);
            } catch (IOException e) {
                throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
            } catch (Exception e) {
                throw new IllegalStateException("Não foi possível ler " + ARQUIVO, e);
            }
        }
    }
}
