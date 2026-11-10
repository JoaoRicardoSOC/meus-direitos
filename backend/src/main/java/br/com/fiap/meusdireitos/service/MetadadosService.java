package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dto.response.OpcaoResponse;
import br.com.fiap.meusdireitos.model.enums.EnumDescritivo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista os valores possíveis dos enums para os campos de seleção do front-end.
 */
public class MetadadosService {

    /**
     * @param tipo classe de um enum que implementa {@link EnumDescritivo}
     * @return todas as opções do enum, na ordem em que foram declaradas
     */
    public <E extends Enum<E> & EnumDescritivo> List<OpcaoResponse> listar(Class<E> tipo) {
        List<OpcaoResponse> opcoes = new ArrayList<>();
        for (E valor : tipo.getEnumConstants()) {
            opcoes.add(OpcaoResponse.de(valor));
        }
        return opcoes;
    }
}
