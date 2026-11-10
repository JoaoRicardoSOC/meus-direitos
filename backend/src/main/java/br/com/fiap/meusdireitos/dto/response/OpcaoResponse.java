package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.enums.EnumDescritivo;

/**
 * Valor de enum para exibir na tela, ex.: {"codigo": "ASFALTO", "descricao": "Asfalto"}.
 * O front envia o {@code codigo} de volta e mostra a {@code descricao}.
 */
public record OpcaoResponse(String codigo, String descricao) {

    /** @return opção correspondente ao enum, ou null quando o valor é nulo */
    public static OpcaoResponse de(EnumDescritivo valor) {
        return valor == null ? null : new OpcaoResponse(valor.name(), valor.getDescricao());
    }
}
