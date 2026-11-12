import { useId } from 'react';

/**
 * Campo do protótipo: rótulo em negrito, entrada branca arredondada e erro logo abaixo.
 * `tipo` = "input" (padrão), "textarea" ou "select" (as opções vêm em children).
 * Os demais props vão para a entrada (value, onChange, type, placeholder, autoComplete...).
 */
export default function Campo({ rotulo, erro, ajuda, tipo = 'input', children, className = '', ...props }) {
  const id = useId();
  const idErro = `${id}-erro`;
  const idAjuda = `${id}-ajuda`;
  const descritoPor = [erro ? idErro : null, ajuda ? idAjuda : null].filter(Boolean).join(' ') || undefined;

  const comum = {
    id,
    'aria-invalid': erro ? true : undefined,
    'aria-describedby': descritoPor,
    ...props,
  };

  let entrada;
  if (tipo === 'textarea') {
    entrada = <textarea className={`form-control md-entrada ${erro ? 'is-invalid' : ''}`} rows={4} {...comum} />;
  } else if (tipo === 'select') {
    entrada = (
      <select className={`form-select md-entrada ${erro ? 'is-invalid' : ''}`} {...comum}>
        {children}
      </select>
    );
  } else {
    entrada = <input className={`form-control md-entrada ${erro ? 'is-invalid' : ''}`} {...comum} />;
  }

  return (
    <div className={`md-campo ${className}`}>
      <label htmlFor={id} className="md-campo-rotulo">
        {rotulo}
      </label>
      {entrada}
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
