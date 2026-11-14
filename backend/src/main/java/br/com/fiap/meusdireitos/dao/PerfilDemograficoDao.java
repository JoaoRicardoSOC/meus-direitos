package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.PerfilDemografico;

import java.sql.SQLException;

/**
 * Acesso ao perfil demográfico (TB_GS26S2_PERFIL_DEMOGRAFICO), em relação 1:1 com o usuário.
 * Não usa {@link CrudDao}: a chave é o id do usuário e o perfil é gravado com "upsert".
 */
public interface PerfilDemograficoDao {

    /** @return o perfil do usuário, ou null se ele ainda não respondeu o questionário */
    PerfilDemografico buscar(Long idUsuario) throws SQLException;

    boolean existe(Long idUsuario) throws SQLException;

    /** Atualiza o perfil se já existir; caso contrário, insere. */
    void salvar(PerfilDemografico perfil) throws SQLException;

    /** @return true se havia perfil e ele foi excluído */
    boolean excluir(Long idUsuario) throws SQLException;
}
