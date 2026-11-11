package br.com.fiap.meusdireitos.dto.request;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;

/**
 * Dados da tela "Nova denúncia" (e da edição). Cidade e UF vêm do cadastro do autor.
 *
 * @param imagemUrl caminho da imagem de demonstração escolhida no front (sem upload)
 */
public record DenunciaRequest(String titulo, String descricao, CategoriaDenuncia categoria, String endereco,
                              String bairro, String imagemUrl) {
}
