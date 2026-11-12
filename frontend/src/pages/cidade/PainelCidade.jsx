import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { SeloStatus } from '../../components/CardDenuncia.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf, numero, plural } from '../../utils/formatos.js';
import './painel.css';

const MAX_BAIRROS = 8;

function porcentagem(valor) {
  return `${numero(Math.round(valor ?? 0))}%`;
}

/**
 * Painel da cidade (sem tela no Figma; segue o estilo do feed): os relatórios da API (docs/api.md, 8.9)
 * numa tela só. Sem ?cidade=&uf=, a API usa a cidade de quem está logado.
 * - resumo: GET /relatorios/resumo (obrigatório: sem ele a tela mostra o erro);
 * - por categoria, bairros e engajamento: cada bloco mostra o seu próprio erro, sem derrubar os outros.
 */
export default function PainelCidade() {
  const { usuario } = useSessao();
  const local = useLocation();
  const [dados, setDados] = useState(null); // { resumo, categorias, bairros, engajamento } (cada um { valor } ou { erro })
  const [erro, setErro] = useState('');
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    setDados(null);
    setErro('');
    Promise.allSettled([
      api('/relatorios/resumo'),
      api('/relatorios/categorias'),
      api('/relatorios/bairros?minimo=1'),
      api('/relatorios/engajamento?limite=5'),
    ]).then(([resumo, categorias, bairros, engajamento]) => {
      if (!ativo) return;
      if (resumo.status === 'rejected') {
        setErro(resumo.reason?.message ?? 'Algo deu errado. Tente novamente.');
        return;
      }
      const lista = (r) => (r.status === 'fulfilled' ? { valor: Array.isArray(r.value) ? r.value : [] } : { erro: true });
      setDados({
        resumo: resumo.value,
        categorias: lista(categorias),
        bairros: lista(bairros),
        engajamento: lista(engajamento),
      });
    });
    return () => {
      ativo = false;
    };
  }, [tentativa]);

  // aberto direto pelo endereço: a seta volta para o feed
  const voltarPara = local.key === 'default' ? '/cidade' : undefined;
  const resumo = dados?.resumo;
  const lugar = cidadeUf(resumo?.cidade ?? usuario?.cidade, resumo?.uf ?? usuario?.uf);

  return (
    <Tela titulo="Painel da cidade" comNavegacao gap={16}>
      <Cabecalho titulo="Painel da cidade" voltarPara={voltarPara} />
      <p className="md-corpo">Como está {lugar || 'a sua cidade'}, em números.</p>

      {!dados && !erro && <EsqueletoPainel />}

      {erro && (
        <div className="d-flex flex-column gap-2">
          <Mensagem>{erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={() => setTentativa((t) => t + 1)}>
            Tentar novamente
          </button>
        </div>
      )}

      {resumo && (resumo.denuncias ?? 0) === 0 && (
        <div className="md-card">
          <h2 className="md-subtitulo">Ainda não há denúncias por aqui</h2>
          <p className="md-corpo">Quando os moradores registrarem problemas, os números da cidade aparecem nesta tela.</p>
          <Link to="/denuncias/nova" className="md-botao md-botao-primario">
            Registrar uma denúncia
          </Link>
        </div>
      )}

      {resumo && resumo.denuncias > 0 && (
        <>
          <p className="md-legenda">
            {resumo.denunciasSimuladas > 0
              ? `Os números incluem ${plural(resumo.denunciasSimuladas, 'denúncia', 'denúncias')} de demonstração, de moradores de exemplo criados para a cidade não começar vazia.`
              : 'Todos os números vêm de denúncias de moradores reais.'}
          </p>
          <Resumo resumo={resumo} />
          <PorCategoria estado={dados.categorias} />
          <Bairros estado={dados.bairros} />
          <MaisEngajadas estado={dados.engajamento} />
        </>
      )}
    </Tela>
  );
}

/** Destaque (% resolvidas, o único número em verde) e os demais totais em grade. */
function Resumo({ resumo }) {
  const itens = [
    ['Denúncias', resumo.denuncias],
    ['Em aberto', resumo.abertas],
    ['Em andamento', resumo.emAndamento],
    ['Resolvidas', resumo.resolvidas],
    ['Moradores', resumo.usuarios],
    ['Votos', resumo.votos],
    ['Comentários', resumo.comentarios],
    ['Confirmações de resolvida', resumo.confirmacoesDeResolucao],
  ];

  return (
    <section className="md-card" aria-labelledby="painel-resumo">
      <h2 id="painel-resumo" className="md-painel-titulo">
        Resumo
      </h2>
      <p className="md-painel-destaque">
        <strong>{porcentagem(resumo.percentualResolvidas)}</strong>
        <span>das denúncias foram resolvidas</span>
      </p>
      <dl className="md-painel-grade">
        {itens.map(([rotulo, valor]) => (
          <div key={rotulo} className="md-painel-item">
            <dt>{rotulo}</dt>
            <dd>{numero(valor)}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function ErroDoBloco() {
  return <p className="md-legenda">Não foi possível carregar esta parte agora.</p>;
}

/**
 * Barras horizontais em HTML/CSS. Cada linha tem o número escrito; a barra é só visual
 * (aria-hidden), então o leitor de tela lê a lista: "Asfalto: 5 denúncias, 2 resolvidas".
 */
function PorCategoria({ estado }) {
  const linhas = (estado.valor ?? []).filter((c) => c.total > 0).sort((a, b) => b.total - a.total);
  const maior = Math.max(1, ...linhas.map((c) => c.total));

  return (
    <section className="md-card" aria-labelledby="painel-categorias">
      <h2 id="painel-categorias" className="md-painel-titulo">
        Denúncias por categoria
      </h2>
      {estado.erro && <ErroDoBloco />}
      {!estado.erro && linhas.length === 0 && <p className="md-legenda">Nenhuma denúncia por categoria ainda.</p>}
      {linhas.length > 0 && (
        <ul className="list-unstyled m-0 md-barras">
          {linhas.map((c) => (
            <li key={c.categoria.codigo} className="md-barra-linha">
              <p className="md-barra-texto">
                <span className="md-barra-rotulo">{c.categoria.descricao}</span>
                <span className="md-barra-valor">
                  {numero(c.total)}
                  <span className="md-oculto-visualmente">
                    {' '}
                    {c.total === 1 ? 'denúncia' : 'denúncias'}
                  </span>
                </span>
              </p>
              <span className="md-barra" aria-hidden="true">
                <span className="md-barra-preenchida" style={{ width: `${(c.total / maior) * 100}%` }} />
              </span>
              <p className="md-legenda">
                {plural(c.resolvidas, 'resolvida', 'resolvidas')} ({porcentagem(c.percentualResolvidas)})
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function Bairros({ estado }) {
  const linhas = [...(estado.valor ?? [])].sort((a, b) => b.total - a.total).slice(0, MAX_BAIRROS);

  return (
    <section className="md-card" aria-labelledby="painel-bairros">
      <h2 id="painel-bairros" className="md-painel-titulo">
        Bairros com mais problemas
      </h2>
      {estado.erro && <ErroDoBloco />}
      {!estado.erro && linhas.length === 0 && <p className="md-legenda">Nenhum bairro informado ainda.</p>}
      {linhas.length > 0 && (
        <ol className="md-painel-ranking">
          {linhas.map((b) => (
            <li key={b.bairro}>
              <span className="md-negrito">{b.bairro}</span>
              <span className="md-legenda">
                {plural(b.total, 'denúncia', 'denúncias')} · {plural(b.resolvidas, 'resolvida', 'resolvidas')}
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function MaisEngajadas({ estado }) {
  const linhas = estado.valor ?? [];

  return (
    <section className="md-card" aria-labelledby="painel-engajamento">
      <h2 id="painel-engajamento" className="md-painel-titulo">
        Denúncias com mais participação
      </h2>
      <p className="md-legenda">Somando votos e comentários.</p>
      {estado.erro && <ErroDoBloco />}
      {!estado.erro && linhas.length === 0 && <p className="md-legenda">Nenhuma participação ainda.</p>}
      {linhas.length > 0 && (
        <ol className="md-painel-ranking">
          {linhas.map((d) => (
            <li key={d.id}>
              <Link to={`/denuncias/${d.id}`} className="md-painel-link">
                {d.titulo}
              </Link>
              <span className="d-flex gap-2 align-items-center flex-wrap">
                {d.status && <SeloStatus status={d.status} />}
                <span className="md-legenda">
                  {[
                    d.categoria?.descricao,
                    plural(d.votos, 'voto', 'votos'),
                    plural(d.comentarios, 'comentário', 'comentários'),
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

/** Carregando: blocos chapados com a forma do resumo e das barras. */
function EsqueletoPainel() {
  return (
    <div className="d-flex flex-column gap-3" role="status">
      <span className="md-oculto-visualmente">Carregando o painel da cidade…</span>
      <div className="md-card" aria-hidden="true">
        <span className="md-esqueleto" style={{ width: '30%', height: 16 }} />
        <span className="md-esqueleto" style={{ width: '60%', height: 34 }} />
        <div className="md-painel-grade">
          {Array.from({ length: 8 }, (_, i) => (
            <span key={i} className="md-esqueleto" style={{ height: 40 }} />
          ))}
        </div>
      </div>
      <div className="md-card" aria-hidden="true">
        <span className="md-esqueleto" style={{ width: '50%', height: 16 }} />
        {[90, 70, 45, 30].map((largura) => (
          <span key={largura} className="md-esqueleto" style={{ width: `${largura}%`, height: 12 }} />
        ))}
      </div>
    </div>
  );
}
