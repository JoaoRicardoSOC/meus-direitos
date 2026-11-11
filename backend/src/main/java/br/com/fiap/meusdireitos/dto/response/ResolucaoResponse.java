package br.com.fiap.meusdireitos.dto.response;

/**
 * Progresso da confirmação da comunidade de que o problema foi resolvido.
 * Ex.: {"confirmacoes": 2, "necessarias": 3, "euConfirmei": true} → "2 de 3 moradores confirmaram".
 */
public record ResolucaoResponse(int confirmacoes, int necessarias, boolean euConfirmei) {
}
