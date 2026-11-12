package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.exception.MuitasTentativasException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limita as tentativas de login por e-mail, contra quem tenta adivinhar senhas.
 * <p>
 * Regra: depois de {@value #MAXIMO_FALHAS} falhas seguidas no mesmo e-mail, o login desse e-mail fica bloqueado
 * por {@link #BLOQUEIO} (HTTP 429), mesmo com a senha certa. Um login com sucesso zera o contador.
 * Falhas esquecidas há mais de {@link #BLOQUEIO} também deixam de contar.
 * <p>
 * Fica em memória ({@link ConcurrentHashMap}), sem tabela nova: ao reiniciar a API os contadores zeram,
 * o que é aceitável para o MVP. Entradas vencidas são removidas de tempos em tempos para o mapa não crescer
 * sem limite. O contador vale para e-mails com ou sem conta, para não revelar quais existem.
 */
public class LimiteTentativasLogin {

    static final int MAXIMO_FALHAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    /** Intervalo mínimo entre duas limpezas do mapa. */
    private static final Duration INTERVALO_LIMPEZA = Duration.ofMinutes(1);

    /** Situação de um e-mail: falhas seguidas, momento da última falha e fim do bloqueio (null = livre). */
    private record Situacao(int falhas, Instant ultimaFalha, Instant bloqueadoAte) {
    }

    private final ConcurrentMap<String, Situacao> situacoes = new ConcurrentHashMap<>();
    private final Clock relogio;
    private volatile Instant proximaLimpeza;

    public LimiteTentativasLogin() {
        this(Clock.systemUTC());
    }

    /** Relógio explícito, para os testes avançarem o tempo. */
    LimiteTentativasLogin(Clock relogio) {
        this.relogio = relogio;
        this.proximaLimpeza = relogio.instant().plus(INTERVALO_LIMPEZA);
    }

    /**
     * @param email e-mail já normalizado
     * @throws MuitasTentativasException se o e-mail estiver bloqueado agora
     */
    public void verificar(String email) {
        Instant agora = relogio.instant();
        Situacao s = situacoes.get(email);
        if (s == null || s.bloqueadoAte() == null || !agora.isBefore(s.bloqueadoAte())) {
            return;
        }
        long segundos = Duration.between(agora, s.bloqueadoAte()).toSeconds();
        long minutos = Math.max(1, (segundos + 59) / 60); // arredonda para cima
        throw new MuitasTentativasException("Muitas tentativas de login. Tente de novo em " + minutos
                + (minutos == 1 ? " minuto." : " minutos."));
    }

    /** Registra uma senha errada (ou e-mail inexistente). Na {@value #MAXIMO_FALHAS}ª seguida, bloqueia. */
    public void registrarFalha(String email) {
        Instant agora = relogio.instant();
        limparVencidas(agora);
        situacoes.compute(email, (chave, atual) -> {
            if (atual != null && atual.bloqueadoAte() != null && !vencida(atual, agora)) {
                return atual; // já bloqueado (requisições simultâneas): mantém o bloqueio como está
            }
            int falhas = atual == null || vencida(atual, agora) ? 1 : atual.falhas() + 1;
            Instant bloqueadoAte = falhas >= MAXIMO_FALHAS ? agora.plus(BLOQUEIO) : null;
            return new Situacao(falhas >= MAXIMO_FALHAS ? 0 : falhas, agora, bloqueadoAte);
        });
    }

    /** Login certo: esquece as falhas do e-mail. */
    public void registrarSucesso(String email) {
        situacoes.remove(email);
    }

    /** Quantidade de e-mails acompanhados (para os testes). */
    int tamanho() {
        return situacoes.size();
    }

    /**
     * Uma situação vence quando o bloqueio acabou ou, sem bloqueio, quando a última falha
     * tem mais de {@link #BLOQUEIO}.
     */
    private static boolean vencida(Situacao s, Instant agora) {
        if (s.bloqueadoAte() != null) {
            return !agora.isBefore(s.bloqueadoAte());
        }
        return !agora.isBefore(s.ultimaFalha().plus(BLOQUEIO));
    }

    private void limparVencidas(Instant agora) {
        if (agora.isBefore(proximaLimpeza)) {
            return;
        }
        proximaLimpeza = agora.plus(INTERVALO_LIMPEZA);
        situacoes.entrySet().removeIf(e -> vencida(e.getValue(), agora));
    }
}
