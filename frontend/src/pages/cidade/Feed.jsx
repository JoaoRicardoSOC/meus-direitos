import { useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Avatar from '../../components/Avatar.jsx';
import CardDenuncia from '../../components/CardDenuncia.jsx';
import BotaoNovaDenuncia from '../../components/BotaoNovaDenuncia.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api, query } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf } from '../../utils/formatos.js';
import NumerosCidade from './NumerosCidade.jsx';
import iconePin from '../../assets/icones/pin.svg';
import './cidade.css';

/** Abas do feed. "Para você" não está no Figma: é o pedido 1 do guia e a ordem padrão da API. */
const ABAS = [
  { ordem: 'PARA_VOCE', rotulo: 'Para você' },
  { ordem: 'RECENTES', rotulo: 'Recentes' },
  { ordem: 'MAIS_VOTADOS', rotulo: 'Mais votados' },
];
const TAMANHO_PAGINA = 10;

function primeiroNome(nome) {
  return (nome ?? '').trim().split(/\s+/)[0] ?? '';
}

/**
 * 07 · Cidade / Feed (os estados de voto das telas 08 e 09 ficam no CardDenuncia).
 * GET /denuncias/feed?ordem=&pagina=&tamanho=, com "Carregar mais" enquanto houver `temProxima`.
 */
export default function Feed() {
  const { usuario } = useSessao();
  const { state } = useLocation();
  const [ordem, setOrdem] = useState('PARA_VOCE');
  const [itens, setItens] = useState([]);
  const [pagina, setPagina] = useState(1);
  const [temProxima, setTemProxima] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [carregandoMais, setCarregandoMais] = useState(false);
  const [erro, setErro] = useState('');
  const [erroMais, setErroMais] = useState('');
  const [tentativa, setTentativa] = useState(0);

  const buscar = useCallback(
    (numero) => api(`/denuncias/feed${query({ ordem, pagina: numero, tamanho: TAMANHO_PAGINA })}`),
    [ordem],
  );

  // primeira página: ao abrir a tela, ao trocar de aba e em "Tentar novamente"
  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setErro('');
    setErroMais('');
    buscar(1)
      .then((dados) => {
        if (!ativo) return;
        setItens(dados?.itens ?? []);
        setPagina(1);
        setTemProxima(Boolean(dados?.temProxima));
      })
      .catch((e) => {
        if (!ativo) return;
        setItens([]);
        setTemProxima(false);
        setErro(e.message);
      })
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [buscar, tentativa]);

  async function carregarMais() {
    setCarregandoMais(true);
    setErroMais('');
    try {
      const dados = await buscar(pagina + 1);
      // evita duplicar itens se o feed mudou entre uma página e outra
      setItens((atuais) => {
        const ids = new Set(atuais.map((d) => d.id));
        return [...atuais, ...(dados?.itens ?? []).filter((d) => !ids.has(d.id))];
      });
      setPagina(pagina + 1);
      setTemProxima(Boolean(dados?.temProxima));
    } catch (e) {
      setErroMais(e.message);
    } finally {
      setCarregandoMais(false);
    }
  }

  function atualizarPlacar(id, placar) {
    setItens((atuais) => atuais.map((d) => (d.id === id ? { ...d, placar } : d)));
  }

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

      <h1 className="md-titulo">Nossa cidade</h1>
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
            onClick={() => setOrdem(aba.ordem)}
          >
            {aba.rotulo}
          </button>
        ))}
      </div>

      {carregando && <Carregando texto="Carregando denúncias…" />}

      {!carregando && erro && (
        <div className="d-flex flex-column gap-2">
          <Mensagem>{erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={() => setTentativa((t) => t + 1)}>
            Tentar novamente
          </button>
        </div>
      )}

      {!carregando && !erro && itens.length === 0 && (
        <div className="md-card">
          <h2 className="md-subtitulo">Nenhuma denúncia por aqui ainda</h2>
          <p className="md-corpo">
            Viu algum problema no seu bairro? Toque em “Nova denúncia” e seja a primeira pessoa a contar para a cidade.
          </p>
        </div>
      )}

      {!carregando && itens.length > 0 && (
        <ul className="list-unstyled m-0 d-flex flex-column gap-3" aria-label="Denúncias da cidade">
          {itens.map((denuncia) => (
            <li key={denuncia.id}>
              <CardDenuncia denuncia={denuncia} aoMudarPlacar={(placar) => atualizarPlacar(denuncia.id, placar)} />
            </li>
          ))}
        </ul>
      )}

      {!carregando && temProxima && (
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
