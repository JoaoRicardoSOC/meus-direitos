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
 * @param consentimento  true quando a pessoa concorda em compartilhar os dados para receber orientações
 *                       (LGPD, dados sensíveis). Obrigatório se qualquer outro campo vier preenchido;
 *                       não é gravado
 */
public record PerfilRequest(LocalDate dataNascimento, RacaCor racaCor, Deficiencia deficiencia,
                            PercursoEscolar percursoEscolar, Boolean baixaRenda, Boolean consentimento) {

    /** @return true se algum dado sensível foi informado (o que exige consentimento) */
    public boolean temDadoSensivel() {
        return dataNascimento != null || racaCor != null || deficiencia != null
                || percursoEscolar != null || baixaRenda != null;
    }
}
