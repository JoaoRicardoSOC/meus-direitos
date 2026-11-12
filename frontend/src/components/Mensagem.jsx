/** Mensagem geral no topo de formulários e listas: erro da API, aviso ou sucesso. */
export default function Mensagem({ tipo = 'erro', children }) {
  if (!children) return null;
  return (
    <p className={`md-alerta md-alerta-${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'}>
      {children}
    </p>
  );
}

/** Estado de carregamento acessível. */
export function Carregando({ texto = 'Carregando…' }) {
  return (
    <div className="d-flex align-items-center gap-2 md-legenda" role="status">
      <span className="spinner-border spinner-border-sm text-success" aria-hidden="true" />
      {texto}
    </div>
  );
}
