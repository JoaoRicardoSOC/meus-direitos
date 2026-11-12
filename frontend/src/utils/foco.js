/**
 * Gestão de foco para leitores de tela e teclado.
 * Cada tela marca o seu título principal com data-foco-tela (e tabIndex={-1});
 * ao trocar de tela ou de etapa, o foco vai para ele e o leitor anuncia onde a pessoa está.
 */

/** Foca o título principal da tela (ou o <main>, se não houver título marcado). */
export function focarTitulo() {
  // espera o React pintar a tela nova
  requestAnimationFrame(() => {
    const alvo = document.querySelector('[data-foco-tela]') ?? document.querySelector('main');
    if (!alvo) return;
    if (!alvo.hasAttribute('tabindex')) alvo.setAttribute('tabindex', '-1');
    alvo.focus({ preventScroll: true });
  });
}

/** Leva a página ao topo na hora (sem animação) e foca o título: usado a cada etapa nova. */
export function irParaOTopo() {
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  focarTitulo();
}

/** Depois de um envio inválido, foca o primeiro campo com erro dentro do formulário. */
export function focarPrimeiroErro(formulario) {
  requestAnimationFrame(() => {
    const campo = formulario?.querySelector('[aria-invalid="true"]');
    campo?.focus();
  });
}

/** Respeita a preferência de "reduzir movimento" do sistema em rolagens feitas por código. */
export function comportamentoDeRolagem() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
}
