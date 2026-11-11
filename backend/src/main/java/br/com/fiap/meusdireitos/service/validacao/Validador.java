package br.com.fiap.meusdireitos.service.validacao;

import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação de dados de entrada com acúmulo de erros.
 * <p>
 * Em vez de parar no primeiro problema, junta todos os erros e lança uma única
 * {@link ValidacaoException}. Assim o front-end mostra cada mensagem ao lado do campo certo.
 * Cada campo guarda só o seu primeiro erro (ex.: se está vazio, não adianta dizer que é curto).
 *
 * <pre>
 * new Validador()
 *     .obrigatorio("nome", req.nome()).tamanho("nome", req.nome(), 3, 100)
 *     .email("email", req.email())
 *     .lancarSeHouverErros();
 * </pre>
 */
public class Validador {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private final List<ErroCampo> erros = new ArrayList<>();
    private final Set<String> camposComErro = new HashSet<>();

    /**
     * Garante que o corpo da requisição foi enviado (POST/PUT sem JSON chega como null).
     *
     * @throws ValidacaoException se o corpo for nulo
     */
    public static void corpoObrigatorio(Object corpo) {
        if (corpo == null) {
            throw new ValidacaoException("O corpo da requisição é obrigatório.");
        }
    }

    /**
     * Converte um texto (ex.: parâmetro de URL) para o enum correspondente, sem diferenciar maiúsculas.
     *
     * @return o valor do enum, ou null se o texto for nulo ou vazio
     * @throws ValidacaoException com a lista de valores aceitos, se o texto não corresponder a nenhum valor
     */
    public static <E extends Enum<E>> E converterEnum(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (E constante : tipo.getEnumConstants()) {
            if (constante.name().equalsIgnoreCase(valor.trim())) {
                return constante;
            }
        }
        StringBuilder aceitos = new StringBuilder();
        for (E constante : tipo.getEnumConstants()) {
            aceitos.append(aceitos.length() == 0 ? "" : ", ").append(constante.name());
        }
        throw new ValidacaoException("Valor '" + valor + "' não é aceito.",
                List.of(new ErroCampo(campo, "Valores aceitos: " + aceitos + ".")));
    }

    /**
     * Lê um número inteiro recebido como texto (ex.: ?pagina=2).
     *
     * @return o número, ou {@code padrao} se o texto for nulo ou vazio
     * @throws ValidacaoException se não for um número inteiro entre min e max
     */
    public static int inteiro(String campo, String valor, int padrao, int min, int max) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero >= min && numero <= max) {
                return numero;
            }
        } catch (NumberFormatException e) {
            // tratado abaixo com a mesma mensagem
        }
        throw new ValidacaoException("Parâmetro '" + campo + "' inválido.",
                List.of(new ErroCampo(campo, "Informe um número inteiro entre " + min + " e " + max + ".")));
    }

    /** Valor não pode ser nulo nem, se for texto, vazio/só espaços. */
    public Validador obrigatorio(String campo, Object valor) {
        boolean vazio = valor == null || (valor instanceof String && ((String) valor).isBlank());
        return regra(!vazio, campo, "Campo obrigatório.");
    }

    /** Tamanho do texto (sem espaços nas pontas) entre min e max. Ignora valores nulos. */
    public Validador tamanho(String campo, String valor, int min, int max) {
        if (valor == null) {
            return this;
        }
        int tamanho = valor.trim().length();
        if (tamanho < min) {
            return regra(false, campo, "Deve ter pelo menos " + min + " caracteres.");
        }
        return regra(tamanho <= max, campo, "Deve ter no máximo " + max + " caracteres.");
    }

    /** Formato de e-mail com até 120 caracteres. Ignora valores nulos. */
    public Validador email(String campo, String valor) {
        if (valor == null) {
            return this;
        }
        String limpo = valor.trim();
        return regra(limpo.length() <= 120 && EMAIL.matcher(limpo).matches(), campo, "E-mail em formato inválido.");
    }

    /** Sigla de estado brasileiro válida (ex.: SP). Ignora valores nulos. */
    public Validador uf(String campo, String valor) {
        if (valor == null) {
            return this;
        }
        return regra(UFS.contains(valor.trim().toUpperCase()), campo, "UF inválida. Use a sigla do estado, ex.: SP.");
    }

    /** Endereço web começando com http:// ou https://, com até 300 caracteres. Ignora nulos e vazios. */
    public Validador url(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return this;
        }
        String limpo = valor.trim();
        boolean valido = limpo.length() <= 300 && (limpo.startsWith("https://") || limpo.startsWith("http://"))
                && !limpo.contains(" ");
        return regra(valido, campo, "Informe um endereço começando com https://");
    }

    /** Data anterior a hoje. Ignora valores nulos. */
    public Validador dataNoPassado(String campo, LocalDate valor) {
        if (valor == null) {
            return this;
        }
        return regra(valor.isBefore(LocalDate.now()), campo, "A data deve ser anterior a hoje.");
    }

    /** Número inteiro entre min e max (inclusive). Ignora valores nulos. */
    public Validador intervalo(String campo, Integer valor, int min, int max) {
        if (valor == null) {
            return this;
        }
        return regra(valor >= min && valor <= max, campo, "Deve estar entre " + min + " e " + max + ".");
    }

    /**
     * Regra genérica: registra a mensagem no campo quando a condição for falsa.
     * Não registra um segundo erro para um campo que já tem erro.
     */
    public Validador regra(boolean condicaoValida, String campo, String mensagem) {
        if (!condicaoValida && camposComErro.add(campo)) {
            erros.add(new ErroCampo(campo, mensagem));
        }
        return this;
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    public List<ErroCampo> getErros() {
        return List.copyOf(erros);
    }

    /**
     * @throws ValidacaoException com todos os erros acumulados, se houver algum
     */
    public void lancarSeHouverErros() {
        if (temErros()) {
            throw new ValidacaoException("Verifique os campos destacados.", erros);
        }
    }
}
