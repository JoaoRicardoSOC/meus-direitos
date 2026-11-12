import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigationType } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Avatar from '../../components/Avatar.jsx';
import CardDenuncia from '../../components/CardDenuncia.jsx';
import BotaoNovaDenuncia from '../../components/BotaoNovaDenuncia.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api, query } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf } from '../../utils/formatos.js';
import { comportamentoDeRolagem } from '../../utils/foco.js';
import NumerosCidade from './NumerosCidade.jsx';
import iconePin from '../../assets/icones/pin.svg';
import './cidade.css';

/** Abas do feed. "Para você" não está no Figma: é o pedido 1 do guia e a ordem padrão da API. */
const ABAS = [
  { ordem: 'PARA_VOCE', rotulo: 'Para você' },
  { ordem: 'RECENTES', rotulo: 'Recentes' },
  { ordem: 'MAIS_VOTADOS', rotulo: 'Mais votados' },
];
const ORDEM_PADRAO = 'PARA_VOCE';
const TAMANHO_PAGINA = 10;
const CHAVE_POSICAO = 'md-feed-posicao';

/**
 * Cache do feed por ordem ({ itens, pagina, temProxima }), vivo enquanto o app estiver aberto:
 * ao voltar do detalhe a lista aparece na hora, na mesma posição, e é revalidada em segundo plano.
 */
const cache = new Map();

function lerPosicao() {
  try {
    return JSON.parse(sessionStorage.getItem(CHAVE_POSICAO)) ?? null;
  } catch {
    return null;
  }
}

function salvarPosicao(posicao) {
  try {
    sessionStorage.setItem(CHAVE_POSICAO, JSON.stringify(posicao));
  } catch {
    // sem sessionStorage (aba privada): só não restaura a rolagem
  }
}

function buscar(ordem, pagina) {
  return api(`/denuncias/feed${query({ ordem, pagina, tamanho: TAMANHO_PAGINA })}`);
}

function primeiroNome(nome) {
  return (nome ?? '').trim().split(/\s+/)[0] ?? '';
}

/**
 * 07 · Cidade / Feed (os estados de voto das telas 08 e 09 ficam no CardDenuncia).
 * GET /denuncias/feed?ordem=&pagina=&tamanho=, com "Carregar mais" enquanto houver `temProxima`.
 * Ao trocar de aba, a lista anterior fica na tela (esmaecida) até a nova chegar.
 */
export default function Feed() {
  const { usuario } = useSessao();
  const { state } = useLocation();
  const voltando = useNavigationType() === 'POP';

  // no "voltar", retoma a aba e a posição de antes
  const [posicaoSalva] = useState(() => (voltando ? lerPosicao() : null));
  const [ordem, setOrdem] = useState(() => posicaoSalva?.ordem ?? ORDEM_PADRAO);
  // lista na tela: pode ser de outra ordem enquanto a nova carrega
  const [lista, setLista] = useState(() => {
    const salva = cache.get(ordem);
    return salva ? { ordem, ...salva } : null;
  });
  const [carregando, setCarregando] = useState(true);
  const [carregandoMais, setCarregandoMais] = useState(false);
  const [erro, setErro] = useState('');
  const [erroMais, setErroMais] = useState('');
  const [tentativa, setTentativa] = useState(0);
  const ordemAtual = useRef(ordem);
  ordemAtual.current = ordem;
  const listaNaTela = useRef(lista);
  listaNaTela.current = lista;

  // restaura a rolagem antes da primeira pintura, e guarda a posição ao sair da tela
  useLayoutEffect(() => {
    if (lista && posicaoSalva?.top) {
      window.scrollTo({ top: posicaoSalva.top, behavior: 'instant' });
    }
    return () => salvarPosicao({ ordem: ordemAtual.current, top: window.scrollY });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // mantém o cache em dia (votos, "Carregar mais", revalidação)
  useEffect(() => {
    if (lista) cache.set(lista.ordem, { itens: lista.itens, pagina: lista.pagina, temProxima: lista.temProxima });
  }, [lista]);

  // primeira página: ao abrir a tela, ao trocar de aba e em "Tentar novamente"
  useEffect(() => {
    let ativo = true;
    const salva = cache.get(ordem);
    if (salva) setLista((atual) => (atual?.ordem === ordem ? atual : { ordem, ...salva }));
    setCarregando(true);
    setErro('');
    setErroMais('');
    buscar(ordem, 1)
      .then((dados) => {
        if (!ativo) return;
        const novos = dados?.itens ?? [];
        setLista((atual) => {
          // revalidação com mais de uma página na tela: atualiza os itens já mostrados, sem encolher a lista
          if (atual?.ordem === ordem && atual.pagina > 1) {
            const porId = new Map(novos.map((d) => [d.id, d]));
            return { ...atual, itens: atual.itens.map((d) => porId.get(d.id) ?? d) };
          }
          return { ordem, itens: novos, pagina: 1, temProxima: Boolean(dados?.temProxima) };
        });
      })
      .catch((e) => {
        if (!ativo) return;
        // se a lista desta ordem já está na tela (cache), a falha da revalidação não apaga nada
        if (listaNaTela.current?.ordem === ordem) return;
        setLista(null);
        setErro(e.message);
      })
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [ordem, tentativa]);

  async function carregarMais() {
    if (!lista) return;
    const { ordem: daLista, pagina } = lista;
    setCarregandoMais(true);
    setErroMais('');
    try {
      const dados = await buscar(daLista, pagina + 1);
      // evita duplicar itens se o feed mudou entre uma página e outra
      setLista((atual) => {
        if (atual?.ordem !== daLista) return atual;
        const ids = new Set(atual.itens.map((d) => d.id));
        return {
          ...atual,
          itens: [...atual.itens, ...(dados?.itens ?? []).filter((d) => !ids.has(d.id))],
          pagina: pagina + 1,
          temProxima: Boolean(dados?.temProxima),
        };
      });
    } catch (e) {
      setErroMais(e.message);
    } finally {
      setCarregandoMais(false);
    }
  }

  function atualizarPlacar(id, placar) {
    setLista((atual) => atual && { ...atual, itens: atual.itens.map((d) => (d.id === id ? { ...d, placar } : d)) });
  }

  function escolherAba(evento, nova) {
    evento.currentTarget.scrollIntoView({ inline: 'nearest', block: 'nearest', behavior: comportamentoDeRolagem() });
    setOrdem(nova);
  }

  const itens = lista?.itens ?? [];
  const listaAntiga = Boolean(lista) && lista.ordem !== ordem; // outra aba, esperando a nova chegar
  const temExemplos = itens.some((d) => d.autor?.simulado);

  return (
    <Tela titulo="Nossa cidade" comNavegacao gap={12}>
      <div className="d-flex gap-2 align-items-start">
        <img src={iconePin} alt="" width="24" height="24" />
        <div className="d-flex flex-column gap-1 flex-grow-1" style={{ minWidth: 0 }}>
          <p className="md-sobretitulo md-sobretitulo-apagado">Sua cidade</p>
          <p className="md-negrito">{cidadeUf(usuario?.cidade, usuario?.uf)}</p>
        </div>
        <Avatar nome={usuario?.nome} pequeno para="/perfil" rotulo="Abrir meu perfil" />
      </div>

      <h1 className="md-titulo" tabIndex={-1} data-foco-tela>
        Nossa cidade
      </h1>
      <p className="md-corpo">Problemas reais. Soluções em comunidade.</p>

      {state?.novoCadastro && (
        <Mensagem tipo="info">
          {state.cidadePreparada === false
            ? `Boas-vindas, ${primeiroNome(usuario?.nome)}! Sua cidade ainda está começando: que tal registrar o primeiro problema?`
            : `Boas-vindas, ${primeiroNome(usuario?.nome)}! Veja o que os moradores estão contando sobre a sua cidade.`}
        </Mensagem>
      )}

      <NumerosCidade />

      <div className="md-abas" role="group" aria-label="Ordenar denúncias">
        {ABAS.map((aba) => (
          <button
            key={aba.ordem}
            type="button"
            className={`md-filtro ${ordem === aba.ordem ? 'ativo' : ''}`}
            aria-pressed={ordem === aba.ordem}
            onClick={(e) => escolherAba(e, aba.ordem)}
          >
            {aba.rotulo}
          </button>
        ))}
      </div>

      {!lista && carregando && <EsqueletoFeed />}

      {!lista && !carregando && erro && (
        <div className="d-flex flex-column gap-2">
          <Mensagem>{erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={() => setTentativa((t) => t + 1)}>
            Tentar novamente
          </button>
        </div>
      )}

      {lista && !listaAntiga && itens.length === 0 && (
        <div className="md-card">
          <h2 className="md-subtitulo">Nenhuma denúncia por aqui ainda</h2>
          <p className="md-corpo">Viu algum problema no seu bairro? Seja a primeira pessoa a contar para a cidade.</p>
          <Link to="/denuncias/nova" className="md-botao md-botao-primario">
            Registrar uma denúncia
          </Link>
        </div>
      )}

      {temExemplos && <p className="md-legenda">Algumas denúncias são exemplos de demonstração.</p>}

      {itens.length > 0 && (
        <ul
          className="list-unstyled m-0 d-flex flex-column gap-3 md-feed-lista"
          aria-label="Denúncias da cidade"
          aria-busy={listaAntiga || undefined}
        >
          {itens.map((denuncia) => (
            <li key={denuncia.id}>
              <CardDenuncia
                denuncia={denuncia}
                ocultarParaVoce={lista.ordem === 'PARA_VOCE'}
                aoMudarPlacar={(placar) => atualizarPlacar(denuncia.id, placar)}
              />
            </li>
          ))}
        </ul>
      )}

      {lista && !listaAntiga && lista.temProxima && (
        <div className="d-flex flex-column gap-2">
          {erroMais && <Mensagem>{erroMais}</Mensagem>}
          <button type="button" className="md-botao md-botao-secundario" onClick={carregarMais} disabled={carregandoMais}>
            {carregandoMais ? 'Carregando…' : 'Carregar mais'}
          </button>
        </div>
      )}

      <BotaoNovaDenuncia />
    </Tela>
  );
}

/** Primeira carga: dois cards chapados com a altura dos reais (cabeçalho, foto 16:9, título, ações). */
function EsqueletoFeed() {
  return (
    <div className="d-flex flex-column gap-3" role="status">
      <span className="md-oculto-visualmente">Carregando denúncias…</span>
      {[0, 1].map((i) => (
        <div key={i} className="md-card" aria-hidden="true">
          <div className="d-flex gap-2 align-items-center">
            <span className="md-esqueleto md-esqueleto-avatar" />
            <div className="d-flex flex-column gap-1 flex-grow-1">
              <span className="md-esqueleto" style={{ width: '45%', height: 16 }} />
              <span className="md-esqueleto" style={{ width: '65%', height: 14 }} />
            </div>
          </div>
          <span className="md-esqueleto md-esqueleto-foto" />
          <div className="d-flex flex-column gap-1">
            <span className="md-esqueleto" style={{ width: '100%', height: 20 }} />
            <span className="md-esqueleto" style={{ width: '70%', height: 20 }} />
          </div>
          <div className="d-flex gap-2">
            <span className="md-esqueleto md-esqueleto-acao" />
            <span className="md-esqueleto md-esqueleto-acao" />
            <span className="md-esqueleto md-esqueleto-acao" style={{ width: 64 }} />
          </div>
        </div>
      ))}
    </div>
  );
}
