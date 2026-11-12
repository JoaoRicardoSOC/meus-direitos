import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import { Carregando } from '../../components/Mensagem.jsx';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf } from '../../utils/formatos.js';
import {
  PORTAL_GOV_BR, buscaDaPrefeitura, buscaDeCras, cidadePorNome, cidadePorPosicao, gravarCache, lerCache,
  obterPosicao, portalDaPrefeitura,
} from './localizacao.js';
import iconeSetaDireita from '../../assets/icones/seta-direita.svg';
import './direitos.css';

/** De onde buscar os canais: posição atual do aparelho ou cidade do cadastro. */
const MODO = { POSICAO: 'posicao', CADASTRO: 'cadastro' };
const CHAVE_MODO = 'md:canais-modo';

const MOTIVO_SEM_LOCALIZACAO = {
  negada: 'Você não permitiu o acesso à localização.',
  'sem-suporte': 'Este navegador não informa a localização.',
  indisponivel: 'Não foi possível descobrir sua localização agora.',
};

function lerModo() {
  try {
    const salvo = sessionStorage.getItem(CHAVE_MODO);
    return Object.values(MODO).includes(salvo) ? salvo : null;
  } catch {
    return null;
  }
}

function gravarModo(modo) {
  try {
    if (modo) sessionStorage.setItem(CHAVE_MODO, modo);
    else sessionStorage.removeItem(CHAVE_MODO);
  } catch {
    // armazenamento indisponível: a escolha vale só nesta tela
  }
}

/**
 * Resolve os três canais a partir do modo escolhido.
 * Com "localização atual", se a posição não vier (negada, sem suporte, demora), usa a cidade do
 * cadastro e devolve o motivo para a tela explicar.
 */
async function resolverCanais(usuario, modo) {
  let posicao = null;
  let motivo = null;
  if (modo === MODO.POSICAO) {
    try {
      posicao = await obterPosicao();
    } catch (erro) {
      motivo = erro.message;
    }
  }

  const chave = posicao
    ? `gps:${posicao.lat.toFixed(2)},${posicao.lon.toFixed(2)}`
    : `cadastro:${usuario?.cidade}|${usuario?.uf}`;
  const emCache = lerCache(chave);
  if (emCache) return { ...emCache, motivo };

  let lugar = null;
  try {
    lugar = posicao ? await cidadePorPosicao(posicao) : await cidadePorNome(usuario?.cidade, usuario?.uf);
  } catch {
    // OpenStreetMap indisponível: seguimos com o que temos
  }

  // Sem identificar a cidade, a prefeitura usa a cidade do cadastro
  const cidade = lugar?.cidade ?? usuario?.cidade;
  const uf = lugar?.uf ?? usuario?.uf;
  const prefeitura = lugar
    ? await portalDaPrefeitura(lugar)
    : { url: buscaDaPrefeitura(cidade, uf), oficial: false };
  const cras = buscaDeCras({ lat: posicao?.lat ?? lugar?.lat, lon: posicao?.lon ?? lugar?.lon, cidade, uf });

  const resultado = { origem: posicao ? MODO.POSICAO : MODO.CADASTRO, cidade, uf, prefeitura, cras };
  gravarCache(chave, resultado);
  return { ...resultado, motivo };
}

/** "https://www.campinas.sp.gov.br/" → "campinas.sp.gov.br" */
function dominio(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return url;
  }
}

/**
 * 32 · Direitos / Canais oficiais.
 * Antes de buscar, a pessoa escolhe: localização atual ou cidade do cadastro. Depois, a prefeitura
 * é a dessa cidade, o Gov.br é único e o atendimento presencial procura CRAS próximos.
 * A escolha fica guardada na sessão; "Trocar" permite escolher de novo.
 */
export default function CanaisOficiais() {
  const { usuario } = useSessao();
  const [modo, setModo] = useState(lerModo);
  const [canais, setCanais] = useState(null);

  useEffect(() => {
    if (!modo) return undefined;
    let ativo = true;
    setCanais(null);
    resolverCanais(usuario, modo).then((resultado) => {
      if (ativo) setCanais(resultado);
    });
    return () => {
      ativo = false;
    };
  }, [modo]); // eslint-disable-line react-hooks/exhaustive-deps

  function escolher(novoModo) {
    gravarModo(novoModo);
    setCanais(null);
    setModo(novoModo);
  }

  const cidadeCadastro = cidadeUf(usuario?.cidade, usuario?.uf);
  const localidade = canais ? cidadeUf(canais.cidade, canais.uf) : '';
  const buscando = modo && !canais;
  const motivo = canais?.motivo
    ? `${MOTIVO_SEM_LOCALIZACAO[canais.motivo] ?? MOTIVO_SEM_LOCALIZACAO.indisponivel} `
    : '';

  return (
    <Tela titulo="Canais de consulta" comNavegacao>
      <Cabecalho titulo="Canais de consulta" voltarPara="/direitos" />

      <p className="md-corpo">Confirme os critérios na fonte oficial. Escolha um canal para pesquisar.</p>

      <section className="md-card md-canais-origem" aria-labelledby="canais-origem-titulo">
        <h2 id="canais-origem-titulo" className="md-subtitulo">
          Onde buscar a prefeitura e o CRAS?
        </h2>

        {/* Os três estados ocupam a mesma célula: o card mantém a altura da escolha e os cards de baixo não pulam */}
        <div className="md-canais-estados">
          <div className="md-canais-estado" inert={Boolean(modo)} data-oculto={modo ? '' : undefined}>
            <p className="md-legenda">Com a localização atual, mostramos os canais de onde você está agora.</p>
            <button type="button" className="md-botao md-botao-primario" onClick={() => escolher(MODO.POSICAO)}>
              Usar minha localização atual
            </button>
            <div className="d-flex flex-column gap-1">
              <button
                type="button"
                className="md-botao md-botao-secundario"
                onClick={() => escolher(MODO.CADASTRO)}
                aria-describedby={cidadeCadastro ? 'canais-cidade-cadastro' : undefined}
              >
                Usar a cidade do cadastro
              </button>
              {cidadeCadastro && (
                <p id="canais-cidade-cadastro" className="md-legenda text-center">
                  {cidadeCadastro}
                </p>
              )}
            </div>
          </div>

          {buscando && (
            <div className="md-canais-estado">
              <Carregando
                texto={modo === MODO.POSICAO ? 'Pegando sua localização…' : 'Procurando os canais da sua cidade…'}
              />
            </div>
          )}

          <div className="md-canais-estado">
            {/* Só o resultado é anunciado, não a seção inteira */}
            <p className="md-corpo-tinta" role="status">
              {canais && (canais.origem === MODO.POSICAO ? `Canais de ${localidade}` : `Canais de ${localidade}, a cidade do seu cadastro`)}
            </p>
            {canais && (
              <>
                <p className="md-legenda">
                  {canais.origem === MODO.POSICAO
                    ? 'Pela sua localização atual. Os canais abaixo são desta região.'
                    : `${motivo}Os canais abaixo são da cidade do cadastro.`}
                </p>
                <button type="button" className="md-acao-texto align-self-start" onClick={() => escolher(null)}>
                  Trocar<span className="md-oculto-visualmente"> onde buscar</span>
                </button>
              </>
            )}
          </div>
        </div>
      </section>

      <ul className="md-direito-canais list-unstyled m-0">
        <li>
          <CardCanal
            titulo="Portal da Prefeitura"
            texto="Serviços e benefícios municipais"
            link={canais?.prefeitura.url}
            pendente={modo ? 'Localizando…' : 'Escolha acima onde buscar.'}
            detalhe={
              canais &&
              (canais.prefeitura.oficial
                ? `Prefeitura de ${canais.cidade} · ${dominio(canais.prefeitura.url)}`
                : `Busca pelo portal da Prefeitura de ${localidade}`)
            }
          />
        </li>
        <li>
          <CardCanal titulo="Portal Gov.br" texto="Programas e serviços federais" link={PORTAL_GOV_BR} detalhe="gov.br" />
        </li>
        <li>
          <CardCanal
            titulo="Atendimento presencial"
            texto="Busque o CRAS ou órgão responsável na sua região."
            link={canais?.cras}
            pendente={modo ? 'Localizando…' : 'Escolha acima onde buscar.'}
            detalhe={
              canais && (canais.origem === MODO.POSICAO ? 'CRAS perto de você, no mapa' : `CRAS em ${localidade}, no mapa`)
            }
          />
        </li>
      </ul>

      <p className="md-legenda">
        Os links abrem sites externos. Nenhum pedido é enviado por este protótipo. A localização só é usada se você
        escolher: ela é consultada no OpenStreetMap para achar a prefeitura e os CRAS da região e não fica salva no Meus
        Direitos.
      </p>

      <Link to="/direitos" className="md-botao md-botao-secundario">
        Voltar aos direitos
      </Link>
    </Tela>
  );
}

/** Card de um canal: link externo com seta, ou, antes da escolha, um card apagado e sem ação. */
function CardCanal({ titulo, texto, link, detalhe, pendente }) {
  if (!link) {
    return (
      <div className="md-card md-canal-pendente">
        <h3 className="md-negrito">{titulo}</h3>
        <p className="md-corpo">{texto}</p>
        <p className="md-legenda">{pendente}</p>
      </div>
    );
  }

  return (
    <a href={link} target="_blank" rel="noopener noreferrer" className="md-card md-direito-card md-canal-link">
      <span className="md-canal-conteudo">
        <h3 className="md-negrito">{titulo}</h3>
        <span className="md-corpo">{texto}</span>
        {detalhe && <span className="md-legenda md-direito-destaque">{detalhe}</span>}
        <span className="md-oculto-visualmente">(abre em nova aba)</span>
      </span>
      <img src={iconeSetaDireita} alt="" width="20" height="20" className="md-canal-seta" />
    </a>
  );
}
