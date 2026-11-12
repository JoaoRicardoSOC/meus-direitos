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

  return (
    <Tela titulo="Canais de consulta" comNavegacao>
      <Cabecalho titulo="Canais de consulta" voltarPara="/direitos" />

      <p className="md-sobretitulo">Próximo passo</p>
      <h2 className="md-titulo">Confirme na fonte oficial.</h2>
      <p className="md-corpo">Escolha um canal para pesquisar os critérios.</p>

      <section className="md-card md-canais-origem" aria-labelledby="canais-origem-titulo" aria-live="polite">
        {!modo && (
          <>
            <h3 id="canais-origem-titulo" className="md-negrito">
              Onde buscar a prefeitura e o CRAS?
            </h3>
            <p className="md-legenda">
              Com a localização atual, mostramos os canais de onde você está agora.
            </p>
            <button type="button" className="md-botao md-botao-primario" onClick={() => escolher(MODO.POSICAO)}>
              Usar minha localização atual
            </button>
            <button type="button" className="md-botao md-botao-secundario" onClick={() => escolher(MODO.CADASTRO)}>
              Usar a cidade do cadastro{cidadeCadastro ? ` (${cidadeCadastro})` : ''}
            </button>
          </>
        )}

        {buscando && (
          <>
            <h3 id="canais-origem-titulo" className="md-oculto-visualmente">Buscando os canais</h3>
            <Carregando
              texto={modo === MODO.POSICAO ? 'Pegando sua localização…' : 'Procurando os canais da sua cidade…'}
            />
          </>
        )}

        {canais && (
          <div className="d-flex flex-column gap-2">
            <h3 id="canais-origem-titulo" className="md-oculto-visualmente">Origem dos canais</h3>
            {canais.origem === MODO.POSICAO && (
              <p className="md-legenda">
                Você está em <strong>{localidade}</strong>. Os canais abaixo são desta região.
              </p>
            )}
            {canais.origem === MODO.CADASTRO && (
              <p className="md-legenda">
                {canais.motivo && `${MOTIVO_SEM_LOCALIZACAO[canais.motivo] ?? MOTIVO_SEM_LOCALIZACAO.indisponivel} `}
                Canais da cidade do seu cadastro: <strong>{localidade}</strong>.
              </p>
            )}
            <button type="button" className="md-link-botao align-self-start" onClick={() => escolher(null)}>
              Trocar
            </button>
          </div>
        )}
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

function CardCanal({ titulo, texto, link, detalhe, pendente }) {
  const conteudo = (
    <>
      <h3 className="md-subtitulo">{titulo}</h3>
      <p className="md-corpo">{texto}</p>
      {detalhe && <p className="md-legenda md-direito-destaque">{detalhe}</p>}
    </>
  );

  if (!link) {
    return (
      <div className="md-card md-canal-pendente">
        {conteudo}
        <p className="md-legenda">{pendente}</p>
      </div>
    );
  }

  return (
    <a href={link} target="_blank" rel="noopener noreferrer" className="md-card md-direito-card">
      {conteudo}
      <span className="md-oculto-visualmente">(abre em nova aba)</span>
    </a>
  );
}
