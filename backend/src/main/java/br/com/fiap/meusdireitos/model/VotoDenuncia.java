package br.com.fiap.meusdireitos.model;

import br.com.fiap.meusdireitos.model.enums.TipoVoto;

import java.time.LocalDateTime;

/**
 * Voto de um usuário em uma denúncia (tabela associativa TB_VOTO_DENUNCIA, relação N:N).
 * A chave é o par (usuário, denúncia): cada pessoa tem no máximo um voto por denúncia.
 */
public class VotoDenuncia {

    private Long idUsuario;
    private Long idDenuncia;
    private TipoVoto tipo;
    private LocalDateTime dataVoto;

    public VotoDenuncia() {
    }

    public VotoDenuncia(Long idUsuario, Long idDenuncia, TipoVoto tipo) {
        this.idUsuario = idUsuario;
        this.idDenuncia = idDenuncia;
        this.tipo = tipo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdDenuncia() {
        return idDenuncia;
    }

    public void setIdDenuncia(Long idDenuncia) {
        this.idDenuncia = idDenuncia;
    }

    public TipoVoto getTipo() {
        return tipo;
    }

    public void setTipo(TipoVoto tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getDataVoto() {
        return dataVoto;
    }

    public void setDataVoto(LocalDateTime dataVoto) {
        this.dataVoto = dataVoto;
    }
}
