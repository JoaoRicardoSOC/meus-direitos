package br.com.fiap.meusdireitos.dto.request;

import br.com.fiap.meusdireitos.model.enums.IconeDireito;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.util.List;

/**
 * Cadastro/atualização de um tema do catálogo de direitos, com seus passos e tags.
 *
 * @param ordem posição do tema na tela (opcional, padrão 0)
 * @param tags  públicos a quem o tema se aplica (use TODOS para qualquer pessoa)
 */
public record DireitoRequest(String titulo, String resumo, String descricao, String aviso, String baseLegal,
                             String linkOficial, IconeDireito icone, Integer ordem,
                             List<PassoRequest> passos, List<TagDemografica> tags) {
}
