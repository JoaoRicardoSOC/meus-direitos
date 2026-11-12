import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../services/api.js';
import { useSessao } from '../contexts/SessaoContext.jsx';
import { cidadeUf, haQuantoTempo } from '../utils/formatos.js';
import Avatar from './Avatar.jsx';
import ImagemDenuncia from './ImagemDenuncia.jsx';
import iconeUp from '../assets/icones/up.svg';
import iconeDown from '../assets/icones/down.svg';
import iconeChat from '../assets/icones/chat.svg';
import iconePin from '../assets/icones/pin-pequeno.svg';
import './CardDenuncia.css';

/**
 * Card de denúncia (componente "MD/Denuncia" do Figma).
 * - resumo (feed): título e foto levam ao detalhe;
 * - completo (detalhe): foto maior e endereço.
 * Os votos seguem a seção 8.6 do guia: clicar no voto atual desmarca (DELETE); senão, PUT.
 */
export default function CardDenuncia({ denuncia, completo = false, aoMudarPlacar }) {
  const { usuario } = useSessao();
  const [placar, setPlacar] = useState(denuncia.placar);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  const ehAutor = usuario && denuncia.autor.id === usuario.id;
  const podeVotar = usuario && !ehAutor;
  const linkDetalhe = `/denuncias/${denuncia.id}`;

  async function votar(tipo) {
    if (!podeVotar || enviando) return;
    setEnviando(true);
    setErro('');
    try {
      const desmarcar = placar.meuVoto === tipo;
      const novo = await api(`/denuncias/${denuncia.id}/voto`, desmarcar
        ? { metodo: 'DELETE' }
        : { metodo: 'PUT', corpo: { tipo } });
      setPlacar(novo);
      aoMudarPlacar?.(novo);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  const titulo = <h2 className="md-subtitulo">{denuncia.titulo}</h2>;
  const foto = <ImagemDenuncia imagemUrl={denuncia.imagemUrl} categoria={denuncia.categoria} altura={completo ? 190 : 120} />;

  return (
    <article className="md-card" aria-label={denuncia.titulo}>
      <div className="d-flex gap-2 align-items-start">
        <Avatar nome={denuncia.autor.nome} />
        <div className="d-flex flex-column gap-1 flex-grow-1" style={{ minWidth: 0 }}>
          <p className="md-negrito d-flex align-items-center gap-2 flex-wrap">
            {denuncia.autor.nome}
            {denuncia.autor.simulado && <span className="md-selo md-selo-exemplo">Exemplo</span>}
          </p>
          <p className="md-legenda">
            {cidadeUf(denuncia.cidade, denuncia.uf)} · {haQuantoTempo(denuncia.criadaEm)}
          </p>
        </div>
      </div>

      <div className="d-flex gap-2 flex-wrap">
        <span className="md-selo">
          {denuncia.categoria.descricao} · {denuncia.status.descricao}
        </span>
        {denuncia.relevanteParaMim && <span className="md-selo md-selo-para-voce">Para você</span>}
      </div>

      {completo ? foto : <Link to={linkDetalhe} className="d-block" tabIndex={-1} aria-hidden="true">{foto}</Link>}

      {completo ? titulo : <Link to={linkDetalhe} className="md-card-link">{titulo}</Link>}

      <p className={`md-corpo ${completo ? '' : 'md-card-resumo'}`}>{denuncia.descricao}</p>

      {completo && (
        <p className="md-legenda d-flex gap-2 align-items-start">
          <img src={iconePin} alt="" width="18" height="18" />
          {[denuncia.endereco, denuncia.bairro].filter(Boolean).join(' · ')}
        </p>
      )}

      <div className="d-flex gap-2 flex-wrap">
        <BotaoVoto tipo="UP" icone={iconeUp} total={placar.upvotes} ativo={placar.meuVoto === 'UP'}
          habilitado={podeVotar} ocupado={enviando} aoClicar={votar} rotulo="Apoiar" />
        <BotaoVoto tipo="DOWN" icone={iconeDown} total={placar.downvotes} ativo={placar.meuVoto === 'DOWN'}
          habilitado={podeVotar} ocupado={enviando} aoClicar={votar} rotulo="Discordar" />
        <Link to={`${linkDetalhe}#conversa`} className="md-acao md-acao-comentarios"
          aria-label={`${denuncia.comentarios} comentários`}>
          <img src={iconeChat} alt="" width="20" height="20" />
          {denuncia.comentarios}
        </Link>
      </div>

      {ehAutor && <p className="md-legenda">Você registrou esta denúncia.</p>}
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
      disabled={!habilitado || ocupado}
      aria-pressed={ativo}
      aria-label={`${rotulo}: ${total}`}
    >
      <img src={icone} alt="" width="20" height="20" />
      {total}
    </button>
  );
}
