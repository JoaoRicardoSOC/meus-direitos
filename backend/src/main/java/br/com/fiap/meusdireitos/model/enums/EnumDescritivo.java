package br.com.fiap.meusdireitos.model.enums;

/**
 * Contrato dos enums exibidos na tela: além do código ({@link #name()}), têm um texto amigável.
 * Permite listar qualquer enum como opção de {@code <select>} de forma genérica.
 */
public interface EnumDescritivo {

    /** @return código gravado no banco e trafegado no JSON (ex.: "CALCADA_ACESSIBILIDADE") */
    String name();

    /** @return texto para exibir ao usuário (ex.: "Calçada e acessibilidade") */
    String getDescricao();
}
