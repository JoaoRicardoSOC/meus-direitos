package br.com.fiap.meusdireitos.model;

/**
 * Um passo do "Por onde começar" de um tema de direitos (tabela TB_DIREITO_PASSO).
 * Só existe dentro de um {@link Direito} (composição). É imutável, por isso é um record.
 *
 * @param ordem  posição do passo (1, 2, 3...)
 * @param titulo título curto (ex.: "Consulte sua prefeitura")
 * @param texto  explicação em linguagem simples
 */
public record PassoDireito(int ordem, String titulo, String texto) {
}
