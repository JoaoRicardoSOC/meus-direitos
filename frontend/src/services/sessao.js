/**
 * Sessão do usuário (docs/api.md, seção 4): a API não usa token.
 * Depois do cadastro ou login guardamos o id (enviado no cabeçalho X-Usuario-Id) e o usuário.
 */
const CHAVE_ID = 'usuarioId';
const CHAVE_USUARIO = 'usuario';
const EVENTO = 'md:sessao';

function ler(chave) {
  try {
    return localStorage.getItem(chave);
  } catch {
    return null; // navegação privada ou armazenamento bloqueado
  }
}

export function lerUsuarioId() {
  return ler(CHAVE_ID);
}

export function lerUsuario() {
  const texto = ler(CHAVE_USUARIO);
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

/** Guarda o usuário devolvido por POST /usuarios, POST /auth/login ou PUT /usuarios/me. */
export function iniciarSessao(usuario) {
  try {
    localStorage.setItem(CHAVE_ID, String(usuario.id));
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
  } catch {
    // sem armazenamento: a sessão vale só enquanto a página estiver aberta
  }
  window.dispatchEvent(new Event(EVENTO));
}

/** "Sair da conta", conta excluída ou resposta 401. */
export function encerrarSessao() {
  try {
    localStorage.removeItem(CHAVE_ID);
    localStorage.removeItem(CHAVE_USUARIO);
  } catch {
    // nada a fazer
  }
  window.dispatchEvent(new Event(EVENTO));
}

/** Avisa quando a sessão muda (login, logout, 401). Devolve a função para cancelar. */
export function aoMudarSessao(callback) {
  window.addEventListener(EVENTO, callback);
  window.addEventListener('storage', callback); // outras abas
  return () => {
    window.removeEventListener(EVENTO, callback);
    window.removeEventListener('storage', callback);
  };
}
