import { useEffect, useRef } from 'react';
import Cabecalho from '../../components/Cabecalho.jsx';
import { haQuantoTempo } from '../../utils/formatos.js';

/**
 * 11 · Comentário enviado: confirmação mostrada depois do POST /denuncias/{id}/comentarios.
 * "Voltar à denúncia" (e a seta) devolvem para o detalhe, já na conversa.
 */
export default function ComentarioEnviado({ comentario, aoVoltar }) {
  const titulo = useRef(null);

  // a confirmação substitui a tela: topo na hora e foco no título, para o leitor anunciar o envio
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    titulo.current?.focus({ preventScroll: true });
  }, []);

  return (
    <>
      <Cabecalho titulo="Comentários" aoVoltar={aoVoltar} />
      <div className="md-celebracao d-flex flex-column" style={{ gap: 'inherit' }}>
        <h2 className="md-titulo" ref={titulo} tabIndex={-1}>
          Comentário enviado
        </h2>
        <p className="md-corpo">Sua participação na conversa do bairro ajuda a dar visibilidade ao problema.</p>

        <article className="md-card">
          <p className="md-comentario-meta">
            <span className="md-comentario-autor">{comentario.autor.nome}</span> · {haQuantoTempo(comentario.criadoEm)}
          </p>
          <p className="md-corpo-tinta">{comentario.texto}</p>
          <span className="md-selo">Enviado</span>
        </article>

        <button type="button" className="md-botao md-botao-primario" onClick={aoVoltar}>
          Voltar à denúncia
        </button>
      </div>
    </>
  );
}
