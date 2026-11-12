import { forwardRef, useEffect, useRef, useState } from 'react';
import Campo from '../../components/Campo.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { haQuantoTempo, plural } from '../../utils/formatos.js';
import { tamanho } from '../../utils/validacao.js';
import { focarPrimeiroErro } from '../../utils/foco.js';
import iconeEditar from '../../assets/icones/editar.svg';
import iconeLixeira from '../../assets/icones/lixeira.svg';

const LIMITE = 500;
const validarTexto = (texto) => tamanho(texto, 2, LIMITE);
const RESUMO_ERRO = 'Confira o comentário antes de enviar.';

/** Contador discreto "N/500" abaixo da caixa de texto (o limite também vale no maxLength). */
function Contador({ texto }) {
  return (
    <p className="md-legenda md-contador" aria-hidden="true">
      {texto.length}/{LIMITE}
    </p>
  );
}

/**
 * Card "Conversa do bairro · N" da tela 10: lista de comentários (do mais antigo ao mais novo)
 * e o formulário "Seu comentário". Quem escreveu (`meu: true`) pode editar e excluir.
 */
const ConversaBairro = forwardRef(function ConversaBairro(
  { idDenuncia, comentarios, erroLista, aoEnviar, aoEditar, aoExcluir },
  ref,
) {
  const [texto, setTexto] = useState('');
  const [erroTexto, setErroTexto] = useState('');
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const formulario = useRef(null);
  const titulo = useRef(null);

  async function enviar(e) {
    e.preventDefault();
    setMensagem('');
    const problema = validarTexto(texto);
    setErroTexto(problema);
    if (problema) {
      setMensagem(RESUMO_ERRO);
      focarPrimeiroErro(formulario.current);
      return;
    }

    setEnviando(true);
    try {
      const novo = await api(`/denuncias/${idDenuncia}/comentarios`, { metodo: 'POST', corpo: { texto: texto.trim() } });
      setTexto('');
      aoEnviar(novo);
    } catch (erro) {
      const porCampo = erro.porCampo();
      if (porCampo.texto) {
        setErroTexto(porCampo.texto);
        setMensagem(RESUMO_ERRO);
        focarPrimeiroErro(formulario.current);
      } else setMensagem(erro.message);
    } finally {
      setEnviando(false);
    }
  }

  // depois de excluir, o foco (que estava no comentário removido) vai para o título da conversa
  function excluido(idComentario) {
    aoExcluir(idComentario);
    requestAnimationFrame(() => titulo.current?.focus({ preventScroll: true }));
  }

  return (
    <section id="conversa" ref={ref} className="md-card md-conversa" aria-labelledby="conversa-titulo">
      <h2 id="conversa-titulo" ref={titulo} tabIndex={-1} className="md-subtitulo">
        Conversa do bairro{' '}
        <span className="md-conversa-contagem">· {plural(comentarios.length, 'comentário', 'comentários')}</span>
      </h2>

      {erroLista && <Mensagem>{erroLista}</Mensagem>}

      {!erroLista && comentarios.length === 0 && (
        <p className="md-corpo">Ninguém comentou ainda. Conte o que você sabe sobre este problema.</p>
      )}

      {comentarios.length > 0 && (
        <ul className="list-unstyled m-0 d-flex flex-column gap-3">
          {comentarios.map((comentario) => (
            <Comentario key={comentario.id} comentario={comentario} aoEditar={aoEditar} aoExcluir={excluido} />
          ))}
        </ul>
      )}

      <form ref={formulario} className="d-flex flex-column gap-2" onSubmit={enviar} noValidate>
        <div>
          <Campo
            tipo="textarea"
            rotulo="Seu comentário"
            rows={3}
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              setErroTexto('');
              setMensagem('');
            }}
            erro={erroTexto}
            placeholder="Também notei esse problema."
            maxLength={LIMITE}
          />
          <Contador texto={texto} />
        </div>
        <Mensagem>{mensagem}</Mensagem>
        <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Comentar'}
        </button>
      </form>
    </section>
  );
});

export default ConversaBairro;

/** Um comentário: autor e "há X · editado · exemplo" numa linha, texto e, se for meu, editar/excluir. */
function Comentario({ comentario, aoEditar, aoExcluir }) {
  const [modo, setModo] = useState('ver'); // 'ver' | 'editar' | 'excluir'
  const [texto, setTexto] = useState(comentario.texto);
  const [erroTexto, setErroTexto] = useState('');
  const [erro, setErro] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const formulario = useRef(null);
  const cancelarExclusao = useRef(null);
  const botaoEditar = useRef(null);
  const botaoExcluir = useRef(null);
  const devolverFoco = useRef(null); // ação que abriu o modo atual, para o foco voltar a ela

  useEffect(() => {
    if (modo === 'excluir') cancelarExclusao.current?.focus();
    if (modo === 'ver' && devolverFoco.current) {
      devolverFoco.current.current?.focus();
      devolverFoco.current = null;
    }
  }, [modo]);

  function abrir(novoModo) {
    setTexto(comentario.texto);
    setErroTexto('');
    setErro('');
    setModo(novoModo);
  }

  function voltar() {
    devolverFoco.current = modo === 'editar' ? botaoEditar : botaoExcluir;
    setErro('');
    setModo('ver');
  }

  async function salvar(e) {
    e.preventDefault();
    setErro('');
    const problema = validarTexto(texto);
    setErroTexto(problema);
    if (problema) {
      setErro(RESUMO_ERRO);
      focarPrimeiroErro(formulario.current);
      return;
    }

    setOcupado(true);
    try {
      const atualizado = await api(`/comentarios/${comentario.id}`, { metodo: 'PUT', corpo: { texto: texto.trim() } });
      aoEditar(atualizado);
      devolverFoco.current = botaoEditar;
      setModo('ver');
    } catch (falha) {
      const porCampo = falha.porCampo();
      if (porCampo.texto) {
        setErroTexto(porCampo.texto);
        setErro(RESUMO_ERRO);
        focarPrimeiroErro(formulario.current);
      } else setErro(falha.message);
    } finally {
      setOcupado(false);
    }
  }

  async function excluir() {
    setOcupado(true);
    setErro('');
    try {
      await api(`/comentarios/${comentario.id}`, { metodo: 'DELETE' });
      aoExcluir(comentario.id);
    } catch (falha) {
      setErro(falha.message);
      setOcupado(false);
    }
  }

  const meta = [haQuantoTempo(comentario.criadoEm)];
  if (comentario.editadoEm) meta.push('editado');
  if (comentario.autor.simulado) meta.push('exemplo');

  return (
    <li className="md-comentario d-flex flex-column gap-2">
      <p className="md-comentario-meta">
        <span className="md-comentario-autor">{comentario.autor.nome}</span> · {meta.join(' · ')}
      </p>

      {modo === 'editar' ? (
        <form ref={formulario} className="d-flex flex-column gap-2" onSubmit={salvar} noValidate>
          <div>
            <Campo
              tipo="textarea"
              rotulo="Editar comentário"
              rows={3}
              value={texto}
              onChange={(e) => {
                setTexto(e.target.value);
                setErroTexto('');
                setErro('');
              }}
              erro={erroTexto}
              maxLength={LIMITE}
              autoFocus
            />
            <Contador texto={texto} />
          </div>
          <Mensagem>{erro}</Mensagem>
          <div className="d-flex gap-2">
            <button type="submit" className="md-botao md-botao-primario" disabled={ocupado}>
              {ocupado ? 'Salvando…' : 'Salvar'}
            </button>
            <button type="button" className="md-botao md-botao-secundario" onClick={voltar} disabled={ocupado}>
              Cancelar
            </button>
          </div>
        </form>
      ) : (
        <p className="md-corpo-tinta">{comentario.texto}</p>
      )}

      {comentario.meu && modo === 'ver' && (
        <div className="d-flex gap-4">
          <button type="button" ref={botaoEditar} className="md-acao-texto" onClick={() => abrir('editar')}>
            <img src={iconeEditar} alt="" width="18" height="18" />
            Editar
          </button>
          <button type="button" ref={botaoExcluir} className="md-acao-texto md-perigo" onClick={() => abrir('excluir')}>
            <img src={iconeLixeira} alt="" width="18" height="18" />
            Excluir
          </button>
        </div>
      )}

      {modo === 'excluir' && (
        <div className="d-flex flex-column gap-2" role="group" aria-labelledby={`excluir-${comentario.id}`}>
          <p id={`excluir-${comentario.id}`} className="md-legenda">
            Excluir este comentário? Essa ação não pode ser desfeita.
          </p>
          <div className="d-flex gap-2">
            <button type="button" className="md-botao md-botao-perigo" onClick={excluir} disabled={ocupado}>
              {ocupado ? 'Excluindo…' : 'Excluir'}
            </button>
            <button type="button" ref={cancelarExclusao} className="md-botao md-botao-secundario" onClick={voltar} disabled={ocupado}>
              Cancelar
            </button>
          </div>
        </div>
      )}

      {modo !== 'editar' && <Mensagem>{erro}</Mensagem>}
    </li>
  );
}
