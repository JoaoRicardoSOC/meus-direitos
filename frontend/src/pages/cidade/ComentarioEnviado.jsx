import { useEffect, useRef } from 'react';
import Cabecalho from '../../components/Cabecalho.jsx';
import { haQuantoTempo } from '../../utils/formatos.js';

/**
 * 11 · Comentário enviado: confirmação mostrada depois do POST /denuncias/{id}/comentarios.
 * "Voltar à denúncia" (e a seta) devolvem para o detalhe, já na conversa.
 */
export default function ComentarioEnviado({ comentario, aoVoltar }) {
  const titulo = useRef(null);

  // leva o foco para o título, para quem usa leitor de tela saber que o envio deu certo
  useEffect(() => {
    window.scrollTo(0, 0);
    titulo.current?.focus();
  }, []);

  return (
    <>
      <Cabecalho titulo="Comentários" aoVoltar={aoVoltar} />
      <p className="md-sobretitulo">Conversa do bairro</p>
      <h2 className="md-titulo" ref={titulo} tabIndex={-1} style={{ outline: 'none' }}>
        Comentário enviado
      </h2>
      <p className="md-corpo">Sua participação ajuda a dar visibilidade.</p>

      <article className="md-card">
        <p className="md-negrito">
          {comentario.autor.nome} · {haQuantoTempo(comentario.criadoEm)}
        </p>
        <p className="md-corpo">{comentario.texto}</p>
        <span className="md-selo md-selo-enviado">Enviado</span>
      </article>

      <button type="button" className="md-botao md-botao-primario" onClick={aoVoltar}>
        Voltar à denúncia
      </button>
    </>
  );
}
