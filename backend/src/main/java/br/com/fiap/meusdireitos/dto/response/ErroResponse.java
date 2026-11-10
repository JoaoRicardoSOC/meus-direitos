package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.exception.ErroCampo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo padrão de toda resposta de erro da API.
 *
 * @param status    código HTTP (ex.: 400)
 * @param erro      título curto do erro (ex.: "Dados inválidos")
 * @param mensagem  explicação para mostrar ao usuário
 * @param campos    erros por campo do formulário (lista vazia quando não se aplica)
 * @param caminho   rota chamada (ex.: /api/usuarios)
 * @param timestamp momento do erro
 */
public record ErroResponse(int status, String erro, String mensagem, List<ErroCampo> campos,
                           String caminho, LocalDateTime timestamp) {
}
