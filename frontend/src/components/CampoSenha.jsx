import { useId, useState } from 'react';
import iconeOlho from '../assets/icones/olho.svg';
import iconeOlhoRiscado from '../assets/icones/olho-riscado.svg';

/** Campo de senha com o ícone de olho para mostrar ou esconder o texto (componente "Campo / Senha"). */
export default function CampoSenha({ rotulo = 'Senha', erro, ajuda, className = '', ...props }) {
  const id = useId();
  const [visivel, setVisivel] = useState(false);
  const idErro = `${id}-erro`;
  const idAjuda = `${id}-ajuda`;
  const descritoPor = [erro ? idErro : null, ajuda ? idAjuda : null].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`md-campo ${className}`}>
      <label htmlFor={id} className="md-campo-rotulo">
        {rotulo}
      </label>
      <div className="md-entrada-com-icone">
        <input
          id={id}
          type={visivel ? 'text' : 'password'}
          className={`form-control md-entrada ${erro ? 'is-invalid' : ''}`}
          aria-invalid={erro ? true : undefined}
          aria-describedby={descritoPor}
          {...props}
        />
        <button
          type="button"
          className="md-icone-campo"
          onClick={() => setVisivel((v) => !v)}
          aria-label={visivel ? 'Esconder senha' : 'Mostrar senha'}
          aria-pressed={visivel}
        >
          <img src={visivel ? iconeOlhoRiscado : iconeOlho} alt="" width="20" height="20" />
        </button>
      </div>
      {ajuda && !erro && (
        <p id={idAjuda} className="md-legenda">
          {ajuda}
        </p>
      )}
      {erro && (
        <p id={idErro} className="md-erro-campo">
          {erro}
        </p>
      )}
    </div>
  );
}
