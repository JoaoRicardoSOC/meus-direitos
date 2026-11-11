package br.com.fiap.meusdireitos.dto.request;

import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;

import java.time.LocalDate;

/**
 * Respostas do questionário "Meus Direitos". Todos os campos são opcionais.
 *
 * @param dataNascimento formato AAAA-MM-DD
 * @param baixaRenda     família no CadÚnico (pergunta opcional)
 */
public record PerfilRequest(LocalDate dataNascimento, RacaCor racaCor, Deficiencia deficiencia,
                            PercursoEscolar percursoEscolar, Boolean baixaRenda) {
}
