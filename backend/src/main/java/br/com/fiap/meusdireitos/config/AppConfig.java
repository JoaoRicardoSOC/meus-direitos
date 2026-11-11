package br.com.fiap.meusdireitos.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configurações da aplicação (banco de dados, endereço do servidor e CORS).
 * <p>
 * Cada valor é procurado nesta ordem:
 * <ol>
 *     <li>variável de ambiente (ex.: {@code MD_DB_SENHA});</li>
 *     <li>arquivo {@code db.properties} na pasta onde a API é executada;</li>
 *     <li>valor padrão, quando existir.</li>
 * </ol>
 * Assim as credenciais do Oracle nunca ficam no código nem no repositório.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    /** Arquivo de configuração local (ignorado pelo git). Modelo: db.properties.example. */
    public static final String ARQUIVO_CONFIG = "db.properties";

    private static final Properties PROPRIEDADES = carregarArquivo();

    private AppConfig() {
        // classe utilitária: não deve ser instanciada
    }

    public static String dbUrl() {
        return valor("db.url", "MD_DB_URL", "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL");
    }

    public static String dbUsuario() {
        return valor("db.usuario", "MD_DB_USUARIO", null);
    }

    public static String dbSenha() {
        return valor("db.senha", "MD_DB_SENHA", null);
    }

    public static String serverUri() {
        return valor("server.uri", "MD_SERVER_URI", "http://localhost:8080/api/");
    }

    /**
     * Origens autorizadas a chamar a API pelo navegador (CORS), separadas por vírgula.
     *
     * @return lista de origens, ex.: [http://localhost:5173]
     */
    public static List<String> corsOrigens() {
        String valor = valor("cors.origens", "MD_CORS_ORIGENS", "http://localhost:5173");
        List<String> origens = new ArrayList<>();
        for (String origem : valor.split(",")) {
            if (!origem.isBlank()) {
                origens.add(origem.trim());
            }
        }
        return origens;
    }

    /**
     * Liga a "cidade viva": conteúdo de demonstração para cidades novas e atividade simulada no feed.
     * Padrão: ligada. Use simulacao.ativa=false para desligar (ex.: testes).
     */
    public static boolean simulacaoAtiva() {
        return !"false".equalsIgnoreCase(valor("simulacao.ativa", "MD_SIMULACAO_ATIVA", "true"));
    }

    /**
     * Chave exigida (cabeçalho X-Chave-Simulacao) nas rotas de preparo da demonstração.
     *
     * @return a chave, ou null se não configurada (nesse caso as rotas ficam desabilitadas)
     */
    public static String simulacaoChave() {
        return valor("simulacao.chave", "MD_SIMULACAO_CHAVE", null);
    }

    /**
     * Lista as configurações obrigatórias que não foram informadas.
     *
     * @return nomes das chaves ausentes (vazia quando está tudo configurado)
     */
    public static List<String> pendencias() {
        List<String> faltando = new ArrayList<>();
        if (dbUsuario() == null) {
            faltando.add("db.usuario (ou MD_DB_USUARIO)");
        }
        if (dbSenha() == null) {
            faltando.add("db.senha (ou MD_DB_SENHA)");
        }
        return faltando;
    }

    private static String valor(String chave, String variavelAmbiente, String padrao) {
        String doAmbiente = System.getenv(variavelAmbiente);
        if (doAmbiente != null && !doAmbiente.isBlank()) {
            return doAmbiente.trim();
        }
        String doArquivo = PROPRIEDADES.getProperty(chave);
        if (doArquivo != null && !doArquivo.isBlank()) {
            return doArquivo.trim();
        }
        return padrao;
    }

    private static Properties carregarArquivo() {
        Properties propriedades = new Properties();
        Path arquivo = Path.of(ARQUIVO_CONFIG);
        if (!Files.exists(arquivo)) {
            LOG.info(() -> "Arquivo " + arquivo.toAbsolutePath() + " não encontrado; usando variáveis de ambiente e padrões.");
            return propriedades;
        }
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            propriedades.load(entrada);
            LOG.info(() -> "Configurações carregadas de " + arquivo.toAbsolutePath()
                    + " (chaves: " + Arrays.toString(propriedades.stringPropertyNames().toArray()) + ")");
        } catch (IOException e) {
            LOG.warning(() -> "Não foi possível ler " + arquivo.toAbsolutePath() + ": " + e.getMessage());
        }
        return propriedades;
    }
}
