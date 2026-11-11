package br.com.fiap.meusdireitos.dto.response;

import br.com.fiap.meusdireitos.model.PerfilDemografico;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respostas do questionário, com a idade calculada e os rótulos de cada opção.
 */
public record PerfilResponse(LocalDate dataNascimento, Integer idade, OpcaoResponse racaCor,
                             OpcaoResponse deficiencia, OpcaoResponse percursoEscolar, Boolean baixaRenda,
                             LocalDateTime atualizadoEm) {

    public static PerfilResponse de(PerfilDemografico perfil, LocalDate hoje) {
        return new PerfilResponse(perfil.getDataNascimento(), perfil.calcularIdade(hoje),
                OpcaoResponse.de(perfil.getRacaCor()), OpcaoResponse.de(perfil.getDeficiencia()),
                OpcaoResponse.de(perfil.getPercursoEscolar()), perfil.getBaixaRenda(), perfil.getDataAtualizacao());
    }
}
