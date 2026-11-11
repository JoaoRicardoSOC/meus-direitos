package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.Deficiencia;
import br.com.fiap.meusdireitos.model.enums.PercursoEscolar;
import br.com.fiap.meusdireitos.model.enums.RacaCor;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.EnumSet;
import java.util.Set;

/**
 * Respostas do questionário "Meus Direitos" (tabela TB_PERFIL_DEMOGRAFICO), em relação 1:1 com o usuário.
 * <p>
 * Todos os campos são opcionais: a pessoa escolhe o que informar ("Pular por enquanto" e
 * "Prefiro não informar"). São dados sensíveis (LGPD) e por isso ficam separados do cadastro.
 * <p>
 * Não herda de {@link EntidadeBase}: a chave é o próprio id do usuário.
 */
public class PerfilDemografico {

    public static final int IDADE_IDOSO = 60;      // Estatuto da Pessoa Idosa (Lei 10.741/2003)
    public static final int IDADE_JOVEM_MIN = 15;  // Estatuto da Juventude (Lei 12.852/2013)
    public static final int IDADE_JOVEM_MAX = 29;

    private Long idUsuario;
    private LocalDate dataNascimento;
    private RacaCor racaCor;
    private Deficiencia deficiencia;
    private PercursoEscolar percursoEscolar;
    private Boolean baixaRenda;
    private LocalDateTime dataAtualizacao;

    /**
     * @param hoje data de referência (parâmetro para permitir testes com datas fixas)
     * @return idade em anos completos, ou null se a data de nascimento não foi informada
     */
    public Integer calcularIdade(LocalDate hoje) {
        return dataNascimento == null ? null : Period.between(dataNascimento, hoje).getYears();
    }

    /**
     * Deriva as tags demográficas a partir das respostas.
     * Respostas não informadas (null ou NAO_INFORMADO) nunca geram tag.
     *
     * @param hoje data de referência para o cálculo da idade
     */
    public Set<TagDemografica> calcularTags(LocalDate hoje) {
        Set<TagDemografica> tags = EnumSet.noneOf(TagDemografica.class);

        Integer idade = calcularIdade(hoje);
        if (idade != null) {
            if (idade >= IDADE_IDOSO) {
                tags.add(TagDemografica.IDOSO);
            }
            if (idade >= IDADE_JOVEM_MIN && idade <= IDADE_JOVEM_MAX) {
                tags.add(TagDemografica.JOVEM);
            }
        }
        if (deficiencia != null && deficiencia.isDeficiencia()) {
            tags.add(TagDemografica.PCD);
            if (deficiencia == Deficiencia.FISICA) {
                tags.add(TagDemografica.PCD_MOBILIDADE);
            }
        }
        if (racaCor != null && racaCor.isPpi()) {
            tags.add(TagDemografica.PPI);
        }
        if (percursoEscolar == PercursoEscolar.ESCOLA_PUBLICA) {
            tags.add(TagDemografica.ESCOLA_PUBLICA);
        }
        if (Boolean.TRUE.equals(baixaRenda)) {
            tags.add(TagDemografica.BAIXA_RENDA);
        }
        return tags;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public RacaCor getRacaCor() {
        return racaCor;
    }

    public void setRacaCor(RacaCor racaCor) {
        this.racaCor = racaCor;
    }

    public Deficiencia getDeficiencia() {
        return deficiencia;
    }

    public void setDeficiencia(Deficiencia deficiencia) {
        this.deficiencia = deficiencia;
    }

    public PercursoEscolar getPercursoEscolar() {
        return percursoEscolar;
    }

    public void setPercursoEscolar(PercursoEscolar percursoEscolar) {
        this.percursoEscolar = percursoEscolar;
    }

    public Boolean getBaixaRenda() {
        return baixaRenda;
    }

    public void setBaixaRenda(Boolean baixaRenda) {
        this.baixaRenda = baixaRenda;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
