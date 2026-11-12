import { useCallback, useEffect, useState } from 'react';
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

const MOTIVO_SEM_LOCALIZACAO = {
  negada: 'Você não permitiu o acesso à localização.',
  'sem-suporte': 'Este navegador não informa a localização.',
  indisponivel: 'Não foi possível descobrir sua localização agora.',
};

/**
 * Descobre de onde partir: posição do aparelho ou, sem ela, a cidade do cadastro.
 * Devolve os endereços dos três canais e uma frase explicando a origem.
 */
async function resolverCanais(usuario) {
  let posicao = null;
  let motivo = null;
  try {
    posicao = await obterPosicao();
  } catch (erro) {
    motivo = erro.message;
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

  // Sem conseguir identificar a cidade pela posição, a prefeitura usa a cidade do cadastro
  const cidade = lugar?.cidade ?? usuario?.cidade;
  const uf = lugar?.uf ?? usuario?.uf;
  const prefeitura = lugar
    ? await portalDaPrefeitura(lugar)
    : { url: buscaDaPrefeitura(cidade, uf), oficial: false };
  const cras = buscaDeCras({ lat: posicao?.lat ?? lugar?.lat, lon: posicao?.lon ?? lugar?.lon, cidade, uf });

  const resultado = { origem: posicao ? 'posicao' : 'cadastro', cidade, uf, prefeitura, cras };
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
 * Ao entrar, pede a localização: a prefeitura é a da cidade onde a pessoa está, o Gov.br é único
 * e o atendimento presencial procura CRAS próximos. Sem localização, usa a cidade do cadastro.
 */
export default function CanaisOficiais() {
  const { usuario } = useSessao();
  const [canais, setCanais] = useState(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    setCanais(null);
    resolverCanais(usuario).then((resultado) => {
      if (ativo) setCanais(resultado);
    });
    return () => {
      ativo = false;
    };
    // Refaz só ao pedir de novo a localização; a cidade do cadastro é apenas a reserva.
  }, [tentativa]); // eslint-disable-line react-hooks/exhaustive-deps

  const tentarDeNovo = useCallback(() => setTentativa((n) => n + 1), []);
  const localidade = canais ? cidadeUf(canais.cidade, canais.uf) : '';

  return (
    <Tela titulo="Canais de consulta" comNavegacao>
      <Cabecalho titulo="Canais de consulta" voltarPara="/direitos" />

      <p className="md-sobretitulo">Próximo passo</p>
      <h2 className="md-titulo">Confirme na fonte oficial.</h2>
      <p className="md-corpo">Escolha um canal para pesquisar os critérios.</p>

      <div className="md-informacao md-canais-local" aria-live="polite">
        {!canais && <Carregando texto="Procurando os canais da sua região…" />}
        {canais?.origem === 'posicao' && (
          <p className="md-legenda">
            Você está em <strong>{localidade}</strong>. Os canais abaixo são desta região.
          </p>
        )}
        {canais?.origem === 'cadastro' && (
          <div className="d-flex flex-column gap-2">
            <p className="md-legenda">
              {MOTIVO_SEM_LOCALIZACAO[canais.motivo] ?? MOTIVO_SEM_LOCALIZACAO.indisponivel} Usamos a cidade
              do seu cadastro: <strong>{localidade}</strong>.
            </p>
            <button type="button" className="md-link-botao align-self-start" onClick={tentarDeNovo}>
              Usar minha localização
            </button>
          </div>
        )}
      </div>

      <ul className="md-direito-canais list-unstyled m-0">
        <li>
          <CardCanal
            titulo="Portal da Prefeitura"
            texto="Serviços e benefícios municipais"
            link={canais?.prefeitura.url}
            detalhe={
              canais &&
              (canais.prefeitura.oficial
                ? `Prefeitura de ${canais.cidade} · ${dominio(canais.prefeitura.url)}`
                : `Busca pelo portal da Prefeitura de ${localidade}`)
            }
          />
        </li>
        <li>
          <CardCanal
            titulo="Portal Gov.br"
            texto="Programas e serviços federais"
            link={PORTAL_GOV_BR}
            detalhe="gov.br"
          />
        </li>
        <li>
          <CardCanal
            titulo="Atendimento presencial"
            texto="Busque o CRAS ou órgão responsável na sua região."
            link={canais?.cras}
            detalhe={canais && (canais.origem === 'posicao' ? 'CRAS perto de você, no mapa' : `CRAS em ${localidade}, no mapa`)}
          />
        </li>
      </ul>

      <p className="md-legenda">
        Os links abrem sites externos. Nenhum pedido é enviado por este protótipo. Sua localização serve só para
        achar a prefeitura e os CRAS da região: ela é consultada no OpenStreetMap e não fica salva no Meus Direitos.
      </p>

      <Link to="/direitos" className="md-botao md-botao-secundario">
        Voltar aos direitos
      </Link>
    </Tela>
  );
}

function CardCanal({ titulo, texto, link, detalhe }) {
  const conteudo = (
    <>
      <h3 className="md-subtitulo">{titulo}</h3>
      <p className="md-corpo">{texto}</p>
      {detalhe && <p className="md-legenda md-direito-destaque">{detalhe}</p>}
    </>
  );

  if (!link) {
    return (
      <div className="md-card" aria-busy="true">
        {conteudo}
        <p className="md-legenda">Localizando…</p>
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
