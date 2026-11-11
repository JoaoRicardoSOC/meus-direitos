package br.com.fiap.meusdireitos.dto.response;

import java.util.List;

/**
 * Resposta da tela "Meus Direitos": o perfil, as tags calculadas e os temas aplicáveis.
 */
public record PerfilCompletoResponse(PerfilResponse perfil, List<OpcaoResponse> tags,
                                     List<DireitoResumoResponse> direitos) {
}
