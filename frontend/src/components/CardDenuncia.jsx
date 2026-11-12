import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../services/api.js';
import { useSessao } from '../contexts/SessaoContext.jsx';
import { cidadeUf, haQuantoTempo, numero, plural } from '../utils/formatos.js';
import Avatar from './Avatar.jsx';
import ImagemDenuncia from './ImagemDenuncia.jsx';
import iconeUp from '../assets/icones/up.svg';
import iconeDown from '../assets/icones/down.svg';
import iconeChat from '../assets/icones/chat.svg';
import iconePin from '../assets/icones/pin-pequeno.svg';
import iconeCheck from '../assets/icones/check-pequeno.svg';
import './CardDenuncia.css';

/** Placar previsto logo após o toque (voto otimista): a API confirma ou o voto volta atrás. */
function preverPlacar(placar, tipo, desmarcar) {
  const proximo = { ...placar };
  const contar = (t, delta) => {
    if (t === 'UP') proximo.upvotes += delta;
    if (t === 'DOWN') proximo.downvotes += delta;
  };
  if (placar.meuVoto) contar(placar.meuVoto, -1);
  if (!desmarcar) contar(tipo, 1);
  proximo.meuVoto = desmarcar ? undefined : tipo;
  proximo.saldo = proximo.upvotes - proximo.downvotes;
  return proximo;
}

/** Selo do status com a cor do estado; "Resolvida" leva um check. */
export function SeloStatus({ status }) {
  return (
    <span className={`md-selo md-selo-status-${status.codigo}`}>
      {status.codigo === 'RESOLVIDA' && <img src={iconeCheck} alt="" width="14" height="14" />}
      {status.descricao}
    </span>
  );
}

/**
 * Card de denúncia (componente "MD/Denuncia" do Figma).
 * - resumo (feed): título e foto levam ao detalhe; foto em 16:9;
 * - completo (detalhe): título maior, texto na cor principal, foto em 4:3 e endereço.
 * Votos (docs/api.md, 8.6): tocar no voto atual desmarca (DELETE); senão, PUT. O placar muda na hora
 * do toque e volta ao anterior se a API recusar.
 */
export default function CardDenuncia({ denuncia, completo = false, aoMudarPlacar, ocultarParaVoce = false }) {
  const { usuario } = useSessao();
  const idTitulo = useId();
  const [placar, setPlacar] = useState(denuncia.placar);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  const ehAutor = Boolean(usuario) && denuncia.autor.id === usuario.id;
  const podeVotar = Boolean(usuario) && !ehAutor;
  const linkDetalhe = `/denuncias/${denuncia.id}`;
  const { confirmacoes = 0, necessarias = 3 } = denuncia.resolucao ?? {};
  const mostrarConfirmacoes = denuncia.status.codigo !== 'RESOLVIDA' && confirmacoes > 0;

  async function votar(tipo) {
    if (!podeVotar || enviando) return;
    const anterior = placar;
    const desmarcar = placar.meuVoto === tipo;
    setPlacar(preverPlacar(placar, tipo, desmarcar));
    setEnviando(true);
    setErro('');
    try {
      const novo = await api(`/denuncias/${denuncia.id}/voto`, desmarcar
        ? { metodo: 'DELETE' }
        : { metodo: 'PUT', corpo: { tipo } });
      setPlacar(novo);
      aoMudarPlacar?.(novo);
    } catch (e) {
      setPlacar(anterior);
      setErro(`Não foi possível registrar o voto. ${e.message}`);
    } finally {
      setEnviando(false);
    }
  }

  const meta = [cidadeUf(denuncia.cidade, denuncia.uf), haQuantoTempo(denuncia.criadaEm)];
  if (denuncia.autor.simulado) meta.push('exemplo');

  const titulo = (
    <h2 id={idTitulo} className={completo ? 'md-titulo-detalhe' : 'md-subtitulo'}>
      {denuncia.titulo}
    </h2>
  );
  const foto = (
    <ImagemDenuncia imagemUrl={denuncia.imagemUrl} categoria={denuncia.categoria} proporcao={completo ? '4 / 3' : '16 / 9'} />
  );

  return (
    <article className="md-card" aria-labelledby={idTitulo}>
      <div className="d-flex gap-2 align-items-start">
        <Avatar nome={denuncia.autor.nome} />
        <div className="d-flex flex-column gap-1 flex-grow-1" style={{ minWidth: 0 }}>
          <p className="md-negrito">{denuncia.autor.nome}</p>
          <p className="md-legenda">{meta.join(' · ')}</p>
        </div>
      </div>

      <div className="d-flex gap-2 align-items-center flex-wrap">
        <SeloStatus status={denuncia.status} />
        <span className="md-legenda md-card-categoria">{denuncia.categoria.descricao}</span>
        {denuncia.relevanteParaMim && !ocultarParaVoce && (
          <span className="md-legenda md-card-para-voce">· Relevante para você</span>
        )}
      </div>

      {completo ? foto : <Link to={linkDetalhe} className="d-block" tabIndex={-1} aria-hidden="true">{foto}</Link>}

      {completo ? titulo : <Link to={linkDetalhe} className="md-card-link">{titulo}</Link>}

      <p className={completo ? 'md-corpo-tinta' : 'md-corpo md-card-resumo'}>{denuncia.descricao}</p>

      {completo && (
        <p className="md-legenda d-flex gap-2 align-items-start">
          <img src={iconePin} alt="" width="18" height="18" />
          {[denuncia.endereco, denuncia.bairro].filter(Boolean).join(' · ')}
        </p>
      )}

      {mostrarConfirmacoes && !completo && (
        <p className="md-legenda md-card-confirmacoes">
          {confirmacoes} de {necessarias} moradores confirmaram que foi resolvido
        </p>
      )}

      <div className="d-flex gap-2 flex-wrap">
        <BotaoVoto tipo="UP" icone={iconeUp} total={placar.upvotes} ativo={placar.meuVoto === 'UP'}
          habilitado={podeVotar} ocupado={enviando} aoClicar={votar} rotulo="Apoiar" />
        <BotaoVoto tipo="DOWN" icone={iconeDown} total={placar.downvotes} ativo={placar.meuVoto === 'DOWN'}
          habilitado={podeVotar} ocupado={enviando} aoClicar={votar} rotulo="Discordar" />
        <Link to={`${linkDetalhe}#conversa`} className="md-acao"
          aria-label={plural(denuncia.comentarios, 'comentário', 'comentários')}>
          <img src={iconeChat} alt="" width="20" height="20" />
          {numero(denuncia.comentarios)}
        </Link>
      </div>

      {ehAutor && <p className="md-legenda">Você registrou esta denúncia. Os votos são dos outros moradores.</p>}
      {!usuario && <p className="md-legenda">Entre na sua conta para votar.</p>}
      {erro && <p className="md-erro-campo" role="alert">{erro}</p>}
    </article>
  );
}

function BotaoVoto({ tipo, icone, total, ativo, habilitado, ocupado, aoClicar, rotulo }) {
  return (
    <button
      type="button"
      className={`md-acao ${ativo ? 'ativo' : ''}`}
      onClick={() => aoClicar(tipo)}
      disabled={!habilitado}
      aria-busy={ocupado || undefined}
      aria-pressed={habilitado ? ativo : undefined}
      aria-label={`${rotulo}: ${numero(total)}`}
    >
      <img src={icone} alt="" width="20" height="20" />
      <span aria-hidden="true">
        {rotulo} {numero(total)}
      </span>
    </button>
  );
}
