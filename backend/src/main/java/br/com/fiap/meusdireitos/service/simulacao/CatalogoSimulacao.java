package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Textos usados pela simulação "cidade viva", lidos de {@code simulacao.json} (em resources)
 * com JSON-B. Ficam em arquivo para o grupo poder editar os textos sem mexer em Java.
 */
public record CatalogoSimulacao(List<String> moradores, List<String> logradouros, List<String> bairros,
                                List<Modelo> modelos, List<String> comentarios, List<String> comentariosResolvida,
                                List<String> respostasAutor) {

    /** Modelo de denúncia; {logradouro}, {numero} e {bairro} são preenchidos na geração. */
    public record Modelo(CategoriaDenuncia categoria, String titulo, String descricao) {
    }

    private static final String ARQUIVO = "/simulacao.json";

    /** @return modelos da categoria, na ordem do arquivo */
    public List<Modelo> modelosDa(CategoriaDenuncia categoria) {
        List<Modelo> lista = new ArrayList<>();
        for (Modelo modelo : modelos) {
            if (modelo.categoria() == categoria) {
                lista.add(modelo);
            }
        }
        return lista;
    }

    /**
     * Lê o catálogo do classpath.
     *
     * @throws IllegalStateException se o arquivo não existir ou estiver inválido (erro de empacotamento)
     */
    public static CatalogoSimulacao carregar() {
        try (InputStream entrada = CatalogoSimulacao.class.getResourceAsStream(ARQUIVO);
             Jsonb jsonb = JsonbBuilder.create()) {
            if (entrada == null) {
                throw new IllegalStateException("Arquivo " + ARQUIVO + " não encontrado no classpath");
            }
            return jsonb.fromJson(entrada, CatalogoSimulacao.class);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler " + ARQUIVO, e);
        }
    }
}
