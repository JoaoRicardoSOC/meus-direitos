package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.model.enums.CategoriaDenuncia;
import br.com.fiap.meusdireitos.model.enums.StatusDenuncia;
import br.com.fiap.meusdireitos.model.enums.TipoVoto;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ComentarioPlanejado;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ConfirmacaoPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.DenunciaPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.VotoPlanejado;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Gera o conteúdo de demonstração de uma cidade, sem acessar o banco (por isso é testável com JUnit).
 * <p>
 * Recebe o {@link Random} pronto: com a mesma semente, gera sempre o mesmo pacote (mesma cidade →
 * mesmo conteúdo), o que facilita testar e regravar o pitch.
 * <p>
 * Garantias do pacote:
 * <ul>
 *     <li>contraste para o pitch: asfalto muito votado, calçada/acessibilidade pouco votada;</li>
 *     <li>ninguém vota, comenta como morador ou confirma na própria denúncia; um voto por pessoa;</li>
 *     <li>votos, comentários e confirmações depois da criação do post e antes de "agora";</li>
 *     <li>uma resolvida pelo autor, uma resolvida por 3 confirmações, uma em andamento e
 *         uma aberta com 2 de 3 confirmações (para o avaliador dar a terceira).</li>
 * </ul>
 */
public class GeradorConteudo {

    public static final int MORADORES_POR_CIDADE = 12;
    static final int CONFIRMACOES_PARA_RESOLVER = 3;
    private static final int MAX_DIAS_ATRAS = 20;
    private static final int MIN_MINUTOS_ATRAS = 120;

    /** Quantas denúncias de cada categoria e faixas de votos (apoios e discordâncias). */
    private record PlanoCategoria(CategoriaDenuncia categoria, int quantidade, int upMin, int upMax,
                                  int downMin, int downMax) {
    }

    private static final List<PlanoCategoria> PLANO = List.of(
            new PlanoCategoria(CategoriaDenuncia.ASFALTO, 3, 8, 10, 0, 1),
            new PlanoCategoria(CategoriaDenuncia.CALCADA_ACESSIBILIDADE, 3, 1, 3, 0, 0),
            new PlanoCategoria(CategoriaDenuncia.ILUMINACAO, 2, 3, 6, 0, 1),
            new PlanoCategoria(CategoriaDenuncia.LIMPEZA_URBANA, 2, 3, 6, 0, 1),
            new PlanoCategoria(CategoriaDenuncia.SEGURANCA, 1, 2, 5, 0, 0),
            new PlanoCategoria(CategoriaDenuncia.OUTROS, 1, 1, 4, 0, 1));

    /** Tipos de evento do pulso de atividade, com pesos (em %). */
    public enum TipoEvento { VOTO, COMENTARIO, NOVA_DENUNCIA, CONFIRMACAO }

    private final Random random;
    private final CatalogoSimulacao catalogo;

    public GeradorConteudo(Random random, CatalogoSimulacao catalogo) {
        this.random = random;
        this.catalogo = catalogo;
    }

    /** Semente reproduzível a partir da cidade normalizada e da UF. */
    public static Random randomDaCidade(String cidadeChave, String uf) {
        return new Random((cidadeChave + "/" + uf).hashCode());
    }

    // ------------------------------------------------------------------ pacote inicial

    public PacoteCidade gerarPacote(LocalDateTime agora) {
        LocalDateTime referencia = agora.truncatedTo(ChronoUnit.SECONDS);
        List<String> moradores = sortearMoradores();
        List<DenunciaPlanejada> denuncias = new ArrayList<>();

        int iluminacoes = 0;
        int limpezas = 0;
        for (PlanoCategoria plano : PLANO) {
            List<CatalogoSimulacao.Modelo> modelos = new ArrayList<>(catalogo.modelosDa(plano.categoria()));
            Collections.shuffle(modelos, random);
            for (int i = 0; i < plano.quantidade(); i++) {
                StatusDenuncia status = StatusDenuncia.ABERTA;
                int confirmacoes = 0;
                if (plano.categoria() == CategoriaDenuncia.ILUMINACAO && iluminacoes++ == 0) {
                    status = StatusDenuncia.RESOLVIDA;                 // resolvida pelo próprio autor
                } else if (plano.categoria() == CategoriaDenuncia.LIMPEZA_URBANA) {
                    if (limpezas++ == 0) {
                        status = StatusDenuncia.RESOLVIDA;             // resolvida pela comunidade
                        confirmacoes = CONFIRMACOES_PARA_RESOLVER;
                    } else {
                        confirmacoes = CONFIRMACOES_PARA_RESOLVER - 1; // falta 1: o avaliador pode dar a 3ª
                    }
                } else if (plano.categoria() == CategoriaDenuncia.SEGURANCA) {
                    status = StatusDenuncia.EM_ANDAMENTO;
                }
                denuncias.add(planejarDenuncia(modelos.get(i % modelos.size()), referencia, status, plano,
                        confirmacoes));
            }
        }
        denuncias.sort(Comparator.comparing(DenunciaPlanejada::criadaEm));
        return new PacoteCidade(moradores, denuncias);
    }

    private DenunciaPlanejada planejarDenuncia(CatalogoSimulacao.Modelo modelo, LocalDateTime agora,
                                               StatusDenuncia status, PlanoCategoria plano, int qtConfirmacoes) {
        int autor = random.nextInt(MORADORES_POR_CIDADE);
        LocalDateTime criadaEm = agora.minusMinutes(inteiro(MIN_MINUTOS_ATRAS, MAX_DIAS_ATRAS * 24 * 60));
        String logradouro = escolher(catalogo.logradouros());
        String numero = String.valueOf(inteiro(10, 2000));
        String bairro = escolher(catalogo.bairros());

        List<Integer> outros = outrosMoradores(autor);
        int ups = inteiro(plano.upMin(), plano.upMax());
        int downs = inteiro(plano.downMin(), plano.downMax());
        List<VotoPlanejado> votos = new ArrayList<>();
        for (int i = 0; i < ups + downs && i < outros.size(); i++) {
            votos.add(new VotoPlanejado(outros.get(i), i < ups ? TipoVoto.UP : TipoVoto.DOWN, entre(criadaEm, agora)));
        }

        List<ComentarioPlanejado> comentarios = new ArrayList<>();
        Collections.shuffle(outros, random);
        int qtComentarios = inteiro(1, 3);
        for (int i = 0; i < qtComentarios; i++) {
            comentarios.add(new ComentarioPlanejado(outros.get(i), escolher(catalogo.comentarios()),
                    entre(criadaEm, agora)));
        }
        if (random.nextInt(4) == 0) { // às vezes o autor agradece/responde
            comentarios.add(new ComentarioPlanejado(autor, escolher(catalogo.respostasAutor()), entre(criadaEm, agora)));
        }

        List<ConfirmacaoPlanejada> confirmacoes = new ArrayList<>();
        Collections.shuffle(outros, random);
        for (int i = 0; i < qtConfirmacoes; i++) {
            confirmacoes.add(new ConfirmacaoPlanejada(outros.get(i), entre(criadaEm, agora)));
        }
        if (status == StatusDenuncia.RESOLVIDA) {
            comentarios.add(new ComentarioPlanejado(outros.get(outros.size() - 1),
                    escolher(catalogo.comentariosResolvida()), agora.minusMinutes(inteiro(5, 90))));
        }
        comentarios.sort(Comparator.comparing(ComentarioPlanejado::quando));

        return new DenunciaPlanejada(autor, modelo.categoria(), preencher(modelo.titulo(), logradouro, numero, bairro),
                preencher(modelo.descricao(), logradouro, numero, bairro), logradouro + ", " + numero, bairro,
                imagemDe(modelo.categoria()), criadaEm, status, votos, comentarios, confirmacoes);
    }

    // ------------------------------------------------------------------ pulso de atividade

    /** Sorteia o tipo de evento: voto 50%, comentário 30%, nova denúncia 10%, confirmação 10%. */
    public TipoEvento sortearEvento() {
        int sorteio = random.nextInt(100);
        if (sorteio < 50) {
            return TipoEvento.VOTO;
        }
        if (sorteio < 80) {
            return TipoEvento.COMENTARIO;
        }
        return sorteio < 90 ? TipoEvento.NOVA_DENUNCIA : TipoEvento.CONFIRMACAO;
    }

    /** Nova denúncia "recente" para o pulso (sem votos nem comentários ainda). */
    public DenunciaPlanejada novaDenunciaRecente(LocalDateTime agora, int autor) {
        CategoriaDenuncia categoria = PLANO.get(random.nextInt(PLANO.size())).categoria();
        CatalogoSimulacao.Modelo modelo = escolher(catalogo.modelosDa(categoria));
        String logradouro = escolher(catalogo.logradouros());
        String numero = String.valueOf(inteiro(10, 2000));
        String bairro = escolher(catalogo.bairros());
        return new DenunciaPlanejada(autor, categoria, preencher(modelo.titulo(), logradouro, numero, bairro),
                preencher(modelo.descricao(), logradouro, numero, bairro), logradouro + ", " + numero, bairro,
                imagemDe(categoria), agora.truncatedTo(ChronoUnit.SECONDS).minusMinutes(inteiro(1, 30)),
                StatusDenuncia.ABERTA, List.of(), List.of(), List.of());
    }

    public String comentarioAleatorio() {
        return escolher(catalogo.comentarios());
    }

    // ------------------------------------------------------------------ utilidades

    public int inteiro(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public <T> T escolher(List<T> opcoes) {
        return opcoes.get(random.nextInt(opcoes.size()));
    }

    private List<String> sortearMoradores() {
        List<String> nomes = new ArrayList<>(catalogo.moradores());
        Collections.shuffle(nomes, random);
        return new ArrayList<>(nomes.subList(0, MORADORES_POR_CIDADE));
    }

    private List<Integer> outrosMoradores(int autor) {
        List<Integer> outros = new ArrayList<>();
        for (int i = 0; i < MORADORES_POR_CIDADE; i++) {
            if (i != autor) {
                outros.add(i);
            }
        }
        Collections.shuffle(outros, random);
        return outros;
    }

    /** Instante aleatório entre (início + 1 min) e (fim − 1 min). */
    private LocalDateTime entre(LocalDateTime inicio, LocalDateTime fim) {
        long minutos = ChronoUnit.MINUTES.between(inicio, fim);
        return inicio.plusMinutes(1 + (minutos > 2 ? random.nextInt((int) (minutos - 2)) : 0));
    }

    private static String preencher(String texto, String logradouro, String numero, String bairro) {
        return texto.replace("{logradouro}", logradouro).replace("{numero}", numero).replace("{bairro}", bairro);
    }

    /** Caminho da imagem de demonstração no front: img/demo/calcada-acessibilidade-2.jpg etc. */
    private String imagemDe(CategoriaDenuncia categoria) {
        return "img/demo/" + categoria.name().toLowerCase().replace('_', '-') + "-" + inteiro(1, 3) + ".jpg";
    }
}
