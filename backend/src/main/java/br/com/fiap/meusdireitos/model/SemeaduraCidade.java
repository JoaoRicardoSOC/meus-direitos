package br.com.fiap.meusdireitos.model;

import java.time.LocalDateTime;

/**
 * Registro de que uma cidade recebeu conteúdo de demonstração ("cidade viva") e de quando
 * aconteceu a última atividade simulada ("pulso"). Tabela TB_GS26S2_SEMEADURA_CIDADE.
 *
 * @param cidadeChave     nome normalizado (maiúsculas, sem acento), parte da chave
 * @param cidade          nome como foi digitado (para exibir)
 * @param eventos         total de eventos simulados gerados pelos pulsos
 */
public record SemeaduraCidade(String cidadeChave, String uf, String cidade, LocalDateTime dataSemeadura,
                              LocalDateTime dataUltimoPulso, int eventos) {
}
