import { forwardRef, useState } from 'react';
import Campo from '../../components/Campo.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { haQuantoTempo } from '../../utils/formatos.js';
import { tamanho } from '../../utils/validacao.js';

const validarTexto = (texto) => tamanho(texto, 2, 500);

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

  async function enviar(e) {
    e.preventDefault();
    setMensagem('');
    const problema = validarTexto(texto);
    setErroTexto(problema);
    if (problema) return;

    setEnviando(true);
    try {
      const novo = await api(`/denuncias/${idDenuncia}/comentarios`, { metodo: 'POST', corpo: { texto: texto.trim() } });
      setTexto('');
      aoEnviar(novo);
    } catch (erro) {
      const porCampo = erro.porCampo();
      if (porCampo.texto) setErroTexto(porCampo.texto);
      else setMensagem(erro.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section id="conversa" ref={ref} className="md-card md-conversa" aria-labelledby="conversa-titulo">
      <h2 id="conversa-titulo" className="md-subtitulo">
        Conversa do bairro · {comentarios.length}
      </h2>

      {erroLista && <Mensagem>{erroLista}</Mensagem>}

      {!erroLista && comentarios.length === 0 && (
        <p className="md-corpo">Ninguém comentou ainda. Conte o que você sabe sobre este problema.</p>
      )}

      {comentarios.length > 0 && (
        <ul className="list-unstyled m-0 d-flex flex-column gap-3">
          {comentarios.map((comentario) => (
            <Comentario key={comentario.id} comentario={comentario} aoEditar={aoEditar} aoExcluir={aoExcluir} />
          ))}
        </ul>
      )}

      <form className="d-flex flex-column gap-3" onSubmit={enviar} noValidate>
        <Campo
          tipo="textarea"
          rotulo="Seu comentário"
          rows={3}
          value={texto}
          onChange={(e) => {
            setTexto(e.target.value);
            setErroTexto('');
          }}
          erro={erroTexto}
          placeholder="Também notei esse problema."
          maxLength={500}
        />
        <Mensagem>{mensagem}</Mensagem>
        <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Comentar'}
        </button>
      </form>
    </section>
  );
});

export default ConversaBairro;

/** Um comentário: autor, "há X", texto, selos e, se for meu, editar/excluir. */
function Comentario({ comentario, aoEditar, aoExcluir }) {
  const [modo, setModo] = useState('ver'); // 'ver' | 'editar' | 'excluir'
  const [texto, setTexto] = useState(comentario.texto);
  const [erroTexto, setErroTexto] = useState('');
  const [erro, setErro] = useState('');
  const [ocupado, setOcupado] = useState(false);

  function editar() {
    setTexto(comentario.texto);
    setErroTexto('');
    setErro('');
    setModo('editar');
  }

  async function salvar(e) {
    e.preventDefault();
    setErro('');
    const problema = validarTexto(texto);
    setErroTexto(problema);
    if (problema) return;

    setOcupado(true);
    try {
      const atualizado = await api(`/comentarios/${comentario.id}`, { metodo: 'PUT', corpo: { texto: texto.trim() } });
      aoEditar(atualizado);
      setModo('ver');
    } catch (falha) {
      const porCampo = falha.porCampo();
      if (porCampo.texto) setErroTexto(porCampo.texto);
      else setErro(falha.message);
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

  return (
    <li className="md-comentario d-flex flex-column gap-2">
      <div className="d-flex align-items-center gap-2 flex-wrap">
        <p className="md-negrito">
          {comentario.autor.nome} · {haQuantoTempo(comentario.criadoEm)}
        </p>
        {comentario.autor.simulado && <span className="md-selo md-selo-exemplo">Exemplo</span>}
        {comentario.editadoEm && <span className="md-legenda">editado</span>}
      </div>

      {modo === 'editar' ? (
        <form className="d-flex flex-column gap-2" onSubmit={salvar} noValidate>
          <Campo
            tipo="textarea"
            rotulo="Editar comentário"
            rows={3}
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              setErroTexto('');
            }}
            erro={erroTexto}
            maxLength={500}
            autoFocus
          />
          <div className="d-flex gap-2">
            <button type="submit" className="md-botao md-botao-primario" disabled={ocupado}>
              {ocupado ? 'Salvando…' : 'Salvar'}
            </button>
            <button type="button" className="md-botao md-botao-secundario" onClick={() => setModo('ver')} disabled={ocupado}>
              Cancelar
            </button>
          </div>
        </form>
      ) : (
        <p className="md-corpo">{comentario.texto}</p>
      )}

      {comentario.meu && modo === 'ver' && (
        <div className="d-flex gap-3">
          <button type="button" className="md-link-acao" onClick={editar}>
            Editar
          </button>
          <button type="button" className="md-link-acao md-link-acao-perigo" onClick={() => setModo('excluir')}>
            Excluir
          </button>
        </div>
      )}

      {modo === 'excluir' && (
        <div className="d-flex flex-column gap-2" role="group" aria-label="Confirmar exclusão do comentário">
          <p className="md-legenda">Excluir este comentário? Essa ação não pode ser desfeita.</p>
          <div className="d-flex gap-2">
            <button type="button" className="md-botao md-botao-perigo" onClick={excluir} disabled={ocupado}>
              {ocupado ? 'Excluindo…' : 'Excluir'}
            </button>
            <button type="button" className="md-botao md-botao-secundario" onClick={() => setModo('ver')} disabled={ocupado}>
              Cancelar
            </button>
          </div>
        </div>
      )}

      {erro && (
        <p className="md-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </li>
  );
}
