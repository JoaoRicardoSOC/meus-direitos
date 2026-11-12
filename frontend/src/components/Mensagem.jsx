import { useEffect, useState } from 'react';

/** Mensagem geral no topo de formulários e listas: erro da API, aviso ou sucesso. */
export default function Mensagem({ tipo = 'erro', children }) {
  if (!children) return null;
  return (
    <p className={`md-alerta md-alerta-${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'}>
      {children}
    </p>
  );
}

/**
 * Estado de carregamento acessível. Só aparece depois de 300 ms (classe md-carregando):
 * respostas rápidas não piscam um spinner na tela.
 */
export function Carregando({ texto = 'Carregando…' }) {
  return (
    <div className="d-flex align-items-center gap-2 md-legenda md-carregando" role="status">
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      {texto}
    </div>
  );
}

const TEMPO_NA_TELA_MS = 3500;
const TEMPO_DE_SAIDA_MS = 160; // igual a --md-dur-saida

/**
 * Aviso temporário acima da navegação ("Denúncia marcada como resolvida.").
 * Entra pela borda de baixo, fica 3,5 s e sai pelo mesmo lado. `chave` muda a cada aviso novo,
 * para o mesmo texto poder aparecer de novo. `aoFechar` limpa o aviso na tela que o usa.
 */
export function Toast({ texto, chave, aoFechar }) {
  const [saindo, setSaindo] = useState(false);

  useEffect(() => {
    if (!texto) return undefined;
    setSaindo(false);
    const sair = setTimeout(() => setSaindo(true), TEMPO_NA_TELA_MS);
    const fechar = setTimeout(() => aoFechar?.(), TEMPO_NA_TELA_MS + TEMPO_DE_SAIDA_MS);
    return () => {
      clearTimeout(sair);
      clearTimeout(fechar);
    };
  }, [texto, chave]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!texto) return null;
  return (
    <p className="md-toast m-0" role="status" data-saindo={saindo ? '' : undefined} key={chave}>
      {texto}
    </p>
  );
}
