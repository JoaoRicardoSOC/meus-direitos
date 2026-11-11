package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.ConfirmacaoResolucaoDao;
import br.com.fiap.meusdireitos.dao.DenunciaDao;
import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.ConfirmacaoResolucaoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.response.DenunciaResponse;
import br.com.fiap.meusdireitos.exception.AcessoNegadoException;
import br.com.fiap.meusdireitos.exception.RegraNegocioException;
import br.com.fiap.meusdireitos.model.Denuncia;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;

/**
 * Validação comunitária da resolução: moradores da mesma cidade confirmam que o problema foi
 * consertado. Ao atingir {@link Denuncia#CONFIRMACOES_PARA_RESOLVER} confirmações de pessoas
 * diferentes, a denúncia vira RESOLVIDA automaticamente, na mesma transação.
 * <p>
 * O autor não confirma (ele já pode marcar como resolvida diretamente pelo status).
 */
public class ConfirmacaoResolucaoService {

    /**
     * @return a denúncia atualizada (status e progresso da confirmação)
     * @throws RegraNegocioException (422) se for o autor ou se a denúncia já estiver resolvida
     * @throws AcessoNegadoException (403) se quem confirma for de outra cidade
     */
    public DenunciaResponse confirmar(Long idUsuario, Long idDenuncia) {
        return Transacao.executar(con -> {
            DenunciaDao denunciaDao = new DenunciaDaoJdbc(con);
            Denuncia denuncia = denunciaDao.buscarPorId(idDenuncia); // 404 se não existir

            if (denuncia.isAutor(idUsuario)) {
                throw new RegraNegocioException(
                        "Você registrou esta denúncia: para resolvê-la, use a opção Marcar como resolvida.");
            }
            if (!denuncia.aceitaConfirmacaoDeResolucao()) {
                throw new RegraNegocioException("Esta denúncia já está resolvida.");
            }
            Usuario usuario = new UsuarioDaoJdbc(con).buscarPorId(idUsuario);
            if (!denuncia.ehDaCidade(usuario.getCidade(), usuario.getUf())) {
                throw new AcessoNegadoException("Somente moradores de " + denuncia.getCidade()
                        + " podem confirmar a resolução deste problema.");
            }

            ConfirmacaoResolucaoDao dao = new ConfirmacaoResolucaoDaoJdbc(con);
            dao.confirmar(idUsuario, idDenuncia); // repetir não duplica
            if (Denuncia.confirmacoesSuficientes(dao.contar(idDenuncia))) {
                denunciaDao.atualizarStatus(idDenuncia, StatusDenuncia.RESOLVIDA);
            }
            return DenunciaResponse.de(denunciaDao.buscarDetalhada(idDenuncia, idUsuario));
        });
    }

    /**
     * Retira a própria confirmação (sem erro se a pessoa não tinha confirmado).
     *
     * @throws RegraNegocioException (422) se a denúncia já foi resolvida
     */
    public DenunciaResponse retirar(Long idUsuario, Long idDenuncia) {
        return Transacao.executar(con -> {
            DenunciaDao denunciaDao = new DenunciaDaoJdbc(con);
            Denuncia denuncia = denunciaDao.buscarPorId(idDenuncia); // 404 se não existir
            if (!denuncia.aceitaConfirmacaoDeResolucao()) {
                throw new RegraNegocioException(
                        "A denúncia já foi resolvida; a confirmação não pode mais ser retirada.");
            }
            new ConfirmacaoResolucaoDaoJdbc(con).retirar(idUsuario, idDenuncia);
            return DenunciaResponse.de(denunciaDao.buscarDetalhada(idDenuncia, idUsuario));
        });
    }
}
