import { useEffect, useRef } from 'react';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';

/**
 * 18 · Excluir denúncia: confirmação antes do DELETE /denuncias/{id}.
 * O foco começa em "Manter denúncia": um Enter distraído não apaga nada.
 */
export default function ConfirmarExclusao({ excluindo, erro, aoConfirmar, aoCancelar }) {
  const manterRef = useRef(null);

  useEffect(() => {
    const quadro = requestAnimationFrame(() => manterRef.current?.focus({ preventScroll: true }));
    return () => cancelAnimationFrame(quadro);
  }, []);

  return (
    <Tela titulo="Excluir denúncia">
      <div key="excluir" className="md-etapa">
        <Cabecalho titulo="Gerenciar denúncia" aoVoltar={aoCancelar} />
        <div className="md-espaco-exclusao" />
        <div className="d-flex flex-column gap-2">
          <h2 className="md-titulo" id="titulo-exclusao">
            Excluir esta
            <br />
            denúncia?
          </h2>
          <p className="md-corpo" id="texto-exclusao">
            Ela deixará de aparecer no feed. Os votos, comentários e confirmações dela também serão apagados.
          </p>
        </div>
        <div className="d-flex flex-column gap-3 md-grupo">
          <Mensagem>{erro}</Mensagem>
          <button type="button" className="md-botao md-botao-perigo" onClick={aoConfirmar} disabled={excluindo}>
            {excluindo ? 'Excluindo…' : 'Sim, excluir'}
          </button>
          <button
            ref={manterRef}
            type="button"
            className="md-botao md-botao-secundario"
            onClick={aoCancelar}
            disabled={excluindo}
            aria-describedby="titulo-exclusao texto-exclusao"
          >
            Manter denúncia
          </button>
        </div>
      </div>
    </Tela>
  );
}
