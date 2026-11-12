package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.RelatorioDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.response.RelatorioResponses;
import br.com.fiap.meusdireitos.exception.ErroCampo;
import br.com.fiap.meusdireitos.exception.ValidacaoException;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.relatorio.DenunciaEngajamento;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorBairro;
import br.com.fiap.meusdireitos.model.relatorio.TotalPorCategoria;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.util.ArrayList;
import java.util.List;

/**
 * Relatórios da cidade ("Números da cidade" e rankings), para dar visibilidade aos problemas
 * e medir o impacto (ODS 11).
 * <p>
 * A cidade vem de {@code ?cidade=&uf=} (os dois juntos; só um deles responde 400 no que faltou);
 * se nenhum for informado e houver usuário logado, usa a cidade do cadastro dele.
 */
public class RelatorioService {

    /** Cidade e UF já resolvidas para a consulta. */
    record Cidade(String nome, String uf) {
    }

    public RelatorioResponses.Resumo resumo(String cidade, String uf, Long idLeitor) {
        Cidade alvo = resolverCidade(cidade, uf, idLeitor);
        return Transacao.consultar(con -> RelatorioResponses.Resumo.de(alvo.nome(), alvo.uf(),
                new RelatorioDaoJdbc(con).resumo(alvo.nome(), alvo.uf())));
    }

    public List<RelatorioResponses.PorCategoria> porCategoria(String cidade, String uf, Long idLeitor) {
        Cidade alvo = resolverCidade(cidade, uf, idLeitor);
        return Transacao.consultar(con -> {
            List<RelatorioResponses.PorCategoria> lista = new ArrayList<>();
            for (TotalPorCategoria t : new RelatorioDaoJdbc(con).porCategoria(alvo.nome(), alvo.uf())) {
                lista.add(RelatorioResponses.PorCategoria.de(t));
            }
            return lista;
        });
    }

    public List<RelatorioResponses.PorBairro> porBairro(String cidade, String uf, String minimo, Long idLeitor) {
        Cidade alvo = resolverCidade(cidade, uf, idLeitor);
        int minimoDenuncias = Validador.inteiro("minimo", minimo, 1, 1, 1000);
        return Transacao.consultar(con -> {
            List<RelatorioResponses.PorBairro> lista = new ArrayList<>();
            for (TotalPorBairro t : new RelatorioDaoJdbc(con).porBairro(alvo.nome(), alvo.uf(), minimoDenuncias)) {
                lista.add(RelatorioResponses.PorBairro.de(t));
            }
            return lista;
        });
    }

    public List<RelatorioResponses.Engajamento> engajamento(String cidade, String uf, String limite, Long idLeitor) {
        Cidade alvo = resolverCidade(cidade, uf, idLeitor);
        int quantidade = Validador.inteiro("limite", limite, 5, 1, 20);
        return Transacao.consultar(con -> {
            List<RelatorioResponses.Engajamento> lista = new ArrayList<>();
            for (DenunciaEngajamento d : new RelatorioDaoJdbc(con).engajamento(alvo.nome(), alvo.uf(), quantidade)) {
                lista.add(RelatorioResponses.Engajamento.de(d));
            }
            return lista;
        });
    }

    /**
     * @throws ValidacaoException se a cidade não puder ser determinada ou a UF for inválida
     */
    Cidade resolverCidade(String cidade, String uf, Long idLeitor) {
        boolean temCidade = cidade != null && !cidade.isBlank();
        boolean temUf = uf != null && !uf.isBlank();
        if (temCidade != temUf) {
            // Só um dos dois: avisa o que falta em vez de ignorar o valor e usar a cidade do login
            String faltando = temCidade ? "uf" : "cidade";
            throw new ValidacaoException("Informe a cidade e a UF juntas.",
                    List.of(new ErroCampo(faltando, "Campo obrigatório quando o outro é informado.")));
        }
        if (temCidade) {
            new Validador().tamanho("cidade", cidade, 2, 80).uf("uf", uf).lancarSeHouverErros();
            return new Cidade(cidade.trim(), uf.trim().toUpperCase());
        }
        if (idLeitor != null) {
            Usuario usuario = Transacao.consultar(con -> {
                UsuarioDaoJdbc dao = new UsuarioDaoJdbc(con);
                return dao.existe(idLeitor) ? dao.buscarPorId(idLeitor) : null;
            });
            if (usuario != null) {
                return new Cidade(usuario.getCidade(), usuario.getUf());
            }
        }
        throw new ValidacaoException("Informe a cidade e a UF (ou faça login para usar a sua cidade).",
                List.of(new ErroCampo("cidade", "Campo obrigatório."), new ErroCampo("uf", "Campo obrigatório.")));
    }
}
