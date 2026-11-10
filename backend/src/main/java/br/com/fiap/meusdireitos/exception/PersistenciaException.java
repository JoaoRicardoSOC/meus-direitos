package br.com.fiap.meusdireitos.exception;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Falha ao acessar o banco de dados (HTTP 500, ou 503 quando o banco está fora do ar).
 * <p>
 * O método {@link #de(SQLException)} traduz os códigos de erro do Oracle em exceções da
 * aplicação com mensagens claras para o usuário. Os detalhes técnicos (código ORA, SQL)
 * vão apenas para o log, nunca para a resposta HTTP.
 */
public class PersistenciaException extends MeusDireitosException {

    private static final Logger LOG = Logger.getLogger(PersistenciaException.class.getName());

    // Códigos de erro do Oracle (ORA-xxxxx) tratados de forma específica
    static final int ORA_UNIQUE_VIOLADA = 1;
    static final int ORA_NOT_NULL_VIOLADA = 1400;
    static final int ORA_CHECK_VIOLADA = 2290;
    static final int ORA_PAI_INEXISTENTE = 2291;
    static final int ORA_FILHOS_EXISTENTES = 2292;
    static final int ORA_VALOR_GRANDE_DEMAIS = 12899;
    static final int ORA_USUARIO_SENHA_INVALIDOS = 1017;
    static final int ORA_CONTA_BLOQUEADA = 28000;
    static final int ORA_LISTENER_INDISPONIVEL = 12541;
    static final int ORA_SERVICO_DESCONHECIDO = 12514;
    static final int ORA_SID_DESCONHECIDO = 12505;
    static final int ORA_CONEXAO_RECUSADA = 17002;

    private final boolean bancoIndisponivel;

    public PersistenciaException(String mensagem, Throwable causa, boolean bancoIndisponivel) {
        super(mensagem, causa);
        this.bancoIndisponivel = bancoIndisponivel;
    }

    @Override
    public int getStatusHttp() {
        return bancoIndisponivel ? 503 : 500;
    }

    @Override
    public String getTitulo() {
        return bancoIndisponivel ? "Banco de dados indisponível" : "Erro no banco de dados";
    }

    public boolean isBancoIndisponivel() {
        return bancoIndisponivel;
    }

    /**
     * Converte uma {@link SQLException} na exceção da aplicação mais adequada.
     *
     * @param e exceção lançada pelo driver JDBC
     * @return exceção pronta para ser lançada (o mapper define o status HTTP)
     */
    public static MeusDireitosException de(SQLException e) {
        switch (e.getErrorCode()) {
            case ORA_UNIQUE_VIOLADA:
                return registrar(e, new ConflitoException("Já existe um registro com esses dados."));
            case ORA_PAI_INEXISTENTE:
                return registrar(e, new EntidadeNaoEncontradaException("Registro relacionado não encontrado."));
            case ORA_FILHOS_EXISTENTES:
                return registrar(e, new ConflitoException("Este registro possui vínculos e não pode ser excluído."));
            case ORA_CHECK_VIOLADA:
                return registrar(e, new ValidacaoException("Um dos valores informados não é permitido."));
            case ORA_NOT_NULL_VIOLADA:
                return registrar(e, new ValidacaoException("Um campo obrigatório não foi informado."));
            case ORA_VALOR_GRANDE_DEMAIS:
                return registrar(e, new ValidacaoException("Um dos valores informados é maior que o permitido."));
            default:
                break;
        }

        if (indicaBancoIndisponivel(e)) {
            return registrar(e, new PersistenciaException(
                    "Banco de dados indisponível no momento. Tente novamente em instantes.", e, true));
        }
        return registrar(e, new PersistenciaException(
                "Não foi possível concluir a operação no banco de dados.", e, false));
    }

    private static boolean indicaBancoIndisponivel(SQLException e) {
        if (e instanceof SQLRecoverableException) {
            return true;
        }
        String sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("08")) { // classe 08 = erro de conexão
            return true;
        }
        switch (e.getErrorCode()) {
            case ORA_USUARIO_SENHA_INVALIDOS:
            case ORA_CONTA_BLOQUEADA:
            case ORA_LISTENER_INDISPONIVEL:
            case ORA_SERVICO_DESCONHECIDO:
            case ORA_SID_DESCONHECIDO:
            case ORA_CONEXAO_RECUSADA:
                return true;
            default:
                return false;
        }
    }

    private static MeusDireitosException registrar(SQLException causa, MeusDireitosException traduzida) {
        Level nivel = traduzida.getStatusHttp() >= 500 ? Level.SEVERE : Level.INFO;
        LOG.log(nivel, "SQLException traduzida para " + traduzida.getClass().getSimpleName()
                + " (ORA-" + causa.getErrorCode() + ", SQLState " + causa.getSQLState() + "): "
                + causa.getMessage(), nivel == Level.SEVERE ? causa : null);
        if (traduzida.getCause() == null) {
            traduzida.initCause(causa);
        }
        return traduzida;
    }
}
