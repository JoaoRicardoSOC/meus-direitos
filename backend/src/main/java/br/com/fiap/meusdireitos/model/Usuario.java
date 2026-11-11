package br.com.fiap.meusdireitos.model;

import java.time.LocalDateTime;

/**
 * Cidadão cadastrado na plataforma (tabela TB_USUARIO).
 * <p>
 * Os setters já normalizam os dados (e-mail em minúsculas, UF em maiúsculas, sem espaços
 * nas pontas), garantindo que o mesmo e-mail digitado de formas diferentes seja reconhecido.
 * A senha nunca é guardada em texto puro: apenas o hash ({@link #getSenhaHash()}).
 */
public class Usuario extends EntidadeBase {

    private String nome;
    private String email;
    private String senhaHash;
    private String cidade;
    private String uf;
    private LocalDateTime dataCadastro;
    /** Morador fictício criado pela simulação "cidade viva" (conteúdo de demonstração). */
    private boolean simulado;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String email, String senhaHash, String cidade, String uf,
                   LocalDateTime dataCadastro) {
        setId(id);
        setNome(nome);
        setEmail(email);
        setSenhaHash(senhaHash);
        setCidade(cidade);
        setUf(uf);
        setDataCadastro(dataCadastro);
    }

    /** Normaliza o e-mail: sem espaços nas pontas e em minúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome == null ? null : nome.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizarEmail(email);
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade == null ? null : cidade.trim();
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf == null ? null : uf.trim().toUpperCase();
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public boolean isSimulado() {
        return simulado;
    }

    public void setSimulado(boolean simulado) {
        this.simulado = simulado;
    }
}
