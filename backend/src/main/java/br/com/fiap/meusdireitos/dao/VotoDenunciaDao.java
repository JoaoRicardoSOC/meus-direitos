package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.Placar;
import br.com.fiap.meusdireitos.model.VotoDenuncia;

import java.sql.SQLException;

/**
 * Acesso aos votos (TB_VOTO_DENUNCIA, tabela associativa N:N entre usuário e denúncia).
 */
public interface VotoDenunciaDao {

    /** Cria o voto ou troca o tipo do voto existente (um voto por usuário por denúncia). */
    void salvar(VotoDenuncia voto) throws SQLException;

    /** @return true se havia voto e ele foi removido */
    boolean remover(Long idUsuario, Long idDenuncia) throws SQLException;

    /**
     * @param idLeitor quem está consultando, para informar o próprio voto (null = visitante)
     */
    Placar placar(Long idDenuncia, Long idLeitor) throws SQLException;

    /** Remove todos os votos de uma denúncia (usado ao excluí-la). */
    void excluirPorDenuncia(Long idDenuncia) throws SQLException;

    /** Remove os votos dados pelo usuário e os recebidos nas denúncias dele (exclusão de conta). */
    void excluirDoUsuario(Long idUsuario) throws SQLException;
}
