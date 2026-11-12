package br.com.fiap.meusdireitos.dao.jdbc;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

/**
 * Conversões repetidas entre tipos Java e colunas Oracle, sempre tratando valores nulos.
 * <p>
 * Convenções do projeto: booleanos em CHAR(1) 'S'/'N'; enums gravados pelo nome
 * ({@code Enum.name()}), com CHECK no banco aceitando os mesmos valores.
 */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    // ---------- booleanos (CHAR(1) 'S'/'N') ----------

    public static void setSimNao(PreparedStatement stmt, int indice, Boolean valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.CHAR);
        } else {
            stmt.setString(indice, valor ? "S" : "N");
        }
    }

    public static Boolean getSimNao(ResultSet rs, String coluna) throws SQLException {
        String valor = rs.getString(coluna);
        return valor == null ? null : "S".equals(valor);
    }

    // ---------- datas ----------

    public static void setData(PreparedStatement stmt, int indice, LocalDate valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.DATE);
        } else {
            stmt.setDate(indice, Date.valueOf(valor));
        }
    }

    public static LocalDate getData(ResultSet rs, String coluna) throws SQLException {
        Date valor = rs.getDate(coluna);
        return valor == null ? null : valor.toLocalDate();
    }

    public static void setDataHora(PreparedStatement stmt, int indice, LocalDateTime valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(indice, Timestamp.valueOf(valor));
        }
    }

    public static LocalDateTime getDataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    // ---------- enums (gravados pelo nome) ----------

    public static void setEnum(PreparedStatement stmt, int indice, Enum<?> valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.VARCHAR);
        } else {
            stmt.setString(indice, valor.name());
        }
    }

    public static <E extends Enum<E>> E getEnum(ResultSet rs, String coluna, Class<E> tipo) throws SQLException {
        String valor = rs.getString(coluna);
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }

    // ---------- números que podem ser nulos ----------

    public static void setLong(PreparedStatement stmt, int indice, Long valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.NUMERIC);
        } else {
            stmt.setLong(indice, valor);
        }
    }

    public static Long getLong(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    // ---------- parâmetros em lista (consultas montadas dinamicamente) ----------

    /**
     * Preenche os "?" na ordem da lista. Aceita Long, Integer, String, enums e LocalDateTime.
     * Usado quando a consulta é montada conforme os filtros, para não errar a posição dos parâmetros.
     *
     * @return próximo índice livre
     */
    public static int preencher(PreparedStatement stmt, List<Object> parametros) throws SQLException {
        int i = 1;
        for (Object valor : parametros) {
            if (valor instanceof Long) {
                stmt.setLong(i, (Long) valor);
            } else if (valor instanceof Integer) {
                stmt.setInt(i, (Integer) valor);
            } else if (valor instanceof Enum) {
                stmt.setString(i, ((Enum<?>) valor).name());
            } else if (valor instanceof LocalDateTime) {
                stmt.setTimestamp(i, Timestamp.valueOf((LocalDateTime) valor));
            } else if (valor instanceof String) {
                stmt.setString(i, (String) valor);
            } else {
                throw new IllegalArgumentException("Tipo de parâmetro não suportado: " + valor);
            }
            i++;
        }
        return i;
    }

    // ---------- IN (...) dinâmico ----------

    /**
     * Gera os marcadores de um IN dinâmico: placeholders(3) → "?, ?, ?".
     * Os valores continuam sendo passados com setString/setLong (nunca concatenados no SQL).
     *
     * @param quantidade número de valores (mínimo 1)
     */
    public static String placeholders(int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("Um IN precisa de pelo menos um valor");
        }
        StringJoiner marcadores = new StringJoiner(", ");
        for (int i = 0; i < quantidade; i++) {
            marcadores.add("?");
        }
        return marcadores.toString();
    }

    private static final String COM_ACENTO = "ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
    private static final String SEM_ACENTO = "AAAAAEEEEIIIIOOOOOUUUUCN";

    /**
     * Condição SQL que compara a cidade sem diferenciar maiúsculas nem acentos ("Sao Paulo" = "São Paulo").
     * O valor do "?" deve vir de {@code TextoUtil.chaveCidade}. Os espaços repetidos não são tratados aqui:
     * a API já grava a cidade com {@code TextoUtil.normalizarEspacos}. Assim a expressão só usa funções puras
     * e pode ser indexada (índices IX_DENUNCIA_CIDADE_CHAVE e IX_USUARIO_CIDADE_CHAVE do script.sql, que
     * precisam repetir exatamente esta expressão).
     */
    public static String cidadeIgual(String coluna) {
        return "TRANSLATE(UPPER(TRIM(" + coluna + ")), '" + COM_ACENTO + "', '" + SEM_ACENTO + "') = ?";
    }
}
