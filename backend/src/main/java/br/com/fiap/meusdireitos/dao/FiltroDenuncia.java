package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

/**
 * Filtros opcionais da listagem de denúncias. Campos nulos são ignorados.
 *
 * @param busca trecho procurado no título ou na descrição (sem diferenciar maiúsculas)
 */
public record FiltroDenuncia(String cidade, String uf, CategoriaDenuncia categoria, StatusDenuncia status,
                             String busca) {
}
