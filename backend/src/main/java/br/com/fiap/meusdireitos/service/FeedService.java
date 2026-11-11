package br.com.fiap.meusdireitos.service;

import br.com.fiap.meusdireitos.dao.Transacao;
import br.com.fiap.meusdireitos.dao.jdbc.DenunciaDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.PerfilDemograficoDaoJdbc;
import br.com.fiap.meusdireitos.dao.jdbc.UsuarioDaoJdbc;
import br.com.fiap.meusdireitos.dto.response.DenunciaResponse;
import br.com.fiap.meusdireitos.dto.response.PaginaResponse;
import br.com.fiap.meusdireitos.model.PerfilDemografico;
import br.com.fiap.meusdireitos.model.Usuario;
import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.OrdemFeed;
import br.com.fiap.meusdireitos.model.enums.TagDemografica;
import br.com.fiap.meusdireitos.service.simulacao.SimulacaoService;
import br.com.fiap.meusdireitos.service.validacao.Validador;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Feed da cidade do usuário — o "algoritmo de priorização" do produto.
 * <p>
 * Na ordem "Para você", cada denúncia recebe:
 * <pre>
 * relevância = saldo de votos
 *            + BONUS_RELEVANCIA      se a categoria afeta o perfil do usuário (ex.: calçada → idoso/PCD)
 *            - PENALIDADE_RESOLVIDA  se já foi resolvida (vai para o fim, mas continua visível)
 * </pre>
 * O cálculo é feito no próprio SQL (CASE + SUM); aqui decidimos quais categorias são relevantes.
 * Sem perfil respondido, "Para você" equivale a ordenar pelos votos.
 */
public class FeedService {

    static final int BONUS_RELEVANCIA = 10;
    static final int PENALIDADE_RESOLVIDA = 15; // maior que o bônus: resolvida sempre abaixo das abertas

    public PaginaResponse<DenunciaResponse> feed(Long idUsuario, String ordem, String pagina, String tamanho) {
        OrdemFeed ordemFeed = Validador.converterEnum(OrdemFeed.class, ordem, "ordem");
        OrdemFeed ordemFinal = ordemFeed == null ? OrdemFeed.PARA_VOCE : ordemFeed;
        int numeroPagina = Validador.inteiro("pagina", pagina, 1, 1, DenunciaService.PAGINA_MAXIMA);
        int tamanhoPagina = Validador.inteiro("tamanho", tamanho, DenunciaService.TAMANHO_PADRAO, 1,
                DenunciaService.TAMANHO_MAXIMO);

        // "Cidade viva": antes de montar o feed, gera atividade simulada se já passou o intervalo.
        // Roda em transação própria e nunca lança exceção: o feed não depende dela.
        Usuario leitor = Transacao.consultar(con -> new UsuarioDaoJdbc(con).buscarPorId(idUsuario));
        new SimulacaoService().pulsarSeNecessario(leitor.getCidade(), leitor.getUf());

        return Transacao.consultar(con -> {
            Usuario usuario = new UsuarioDaoJdbc(con).buscarPorId(idUsuario);
            PerfilDemografico perfil = new PerfilDemograficoDaoJdbc(con).buscar(idUsuario);
            Set<TagDemografica> tags = perfil == null
                    ? EnumSet.noneOf(TagDemografica.class) : perfil.calcularTags(LocalDate.now());
            List<CategoriaDenuncia> relevantes = CategoriaDenuncia.relevantesPara(tags);

            return DenunciaService.paraResposta(new DenunciaDaoJdbc(con).feed(usuario.getCidade(), usuario.getUf(),
                    idUsuario, relevantes, ordemFinal, BONUS_RELEVANCIA, PENALIDADE_RESOLVIDA,
                    numeroPagina, tamanhoPagina));
        });
    }
}
