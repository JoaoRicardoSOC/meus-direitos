package br.com.fiap.meusdireitos.dao;

import br.com.fiap.meusdireitos.model.SemeaduraCidade;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controle da simulação por cidade (TB_SEMEADURA_CIDADE).
 */
public interface SemeaduraCidadeDao {

    /**
     * Marca a cidade como semeada. A chave primária impede marcar duas vezes, inclusive quando
     * duas requisições chegam ao mesmo tempo.
     *
     * @return true se a marcação foi feita agora; false se a cidade já estava marcada
     */
    boolean registrar(String cidadeChave, String uf, String cidade) throws SQLException;

    /** @return o registro da cidade, ou null se ela nunca foi semeada */
    SemeaduraCidade buscar(String cidadeChave, String uf) throws SQLException;

    /** Atualiza a data do último pulso e soma os eventos gerados. */
    void registrarPulso(String cidadeChave, String uf, LocalDateTime quando, int eventos) throws SQLException;

    List<SemeaduraCidade> listar() throws SQLException;

    /** Remove todos os registros (reset da demonstração). */
    void excluirTodas() throws SQLException;
}
