package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.VotoDenunciaDao;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.VotoDenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dto.request.VotoRequest;
import br.com.fiap.meusdireitos.dto.response.PlacarResponse;
import br.com.fiap.meusdireitos.exception.RegraNegocioException;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.VotoDenuncia;
import br.com.fiap.meusdireitos.service.validacao.Validador;

/**
 * Votos nas denúncias ("gamificação" da persona Mariana).
 * <p>
 * Regras: um voto por pessoa por denúncia; votar de novo no mesmo tipo não muda nada;
 * votar no tipo oposto troca o voto; ninguém vota na própria denúncia.
 * A resposta é sempre o placar atualizado, para o front atualizar o card na hora.
 */
public class VotoService {

    public PlacarResponse votar(Long idUsuario, Long idDenuncia, VotoRequest req) {
        Validador.corpoObrigatorio(req);
        new Validador().obrigatorio("tipo", req.tipo()).lancarSeHouverErros();

        return Transacao.executar(con -> {
            Denuncia denuncia = new DenunciaDaoJdbc(con).buscarPorId(idDenuncia); // 404 se não existir
            if (denuncia.isAutor(idUsuario)) {
                throw new RegraNegocioException("Você não pode votar na própria denúncia.");
            }
            VotoDenunciaDao dao = new VotoDenunciaDaoJdbc(con);
            dao.salvar(new VotoDenuncia(idUsuario, idDenuncia, req.tipo()));
            return PlacarResponse.de(dao.placar(idDenuncia, idUsuario));
        });
    }

    /** Retira o voto (sem erro se a pessoa não tinha votado). */
    public PlacarResponse removerVoto(Long idUsuario, Long idDenuncia) {
        return Transacao.executar(con -> {
            new DenunciaDaoJdbc(con).buscarPorId(idDenuncia); // 404 se não existir
            VotoDenunciaDao dao = new VotoDenunciaDaoJdbc(con);
            dao.remover(idUsuario, idDenuncia);
            return PlacarResponse.de(dao.placar(idDenuncia, idUsuario));
        });
    }
}
