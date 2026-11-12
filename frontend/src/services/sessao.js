/**
 * Sessão do usuário (docs/api.md, seção 4): a API não usa token.
 * Depois do cadastro ou login guardamos o id (enviado no cabeçalho X-Usuario-Id) e o usuário.
 */
const CHAVE_ID = 'usuarioId';
const CHAVE_USUARIO = 'usuario';
const EVENTO = 'md:sessao';

/**
 * Cópia em memória: se o navegador bloquear o localStorage (navegação privada, site bloqueado),
 * a sessão continua valendo enquanto a página estiver aberta.
 */
const memoria = new Map();

function ler(chave) {
  try {
    const salvo = localStorage.getItem(chave);
    if (salvo !== null) return salvo;
  } catch {
    // armazenamento bloqueado: usa a memória
  }
  return memoria.get(chave) ?? null;
}

function gravar(chave, valor) {
  memoria.set(chave, valor);
  try {
    localStorage.setItem(chave, valor);
  } catch {
    // armazenamento bloqueado: fica só na memória
  }
}

function apagar(chave) {
  memoria.delete(chave);
  try {
    localStorage.removeItem(chave);
  } catch {
    // nada a fazer
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
  gravar(CHAVE_ID, String(usuario.id));
  gravar(CHAVE_USUARIO, JSON.stringify(usuario));
  window.dispatchEvent(new Event(EVENTO));
}

/** "Sair da conta", conta excluída ou resposta 401. */
export function encerrarSessao() {
  apagar(CHAVE_ID);
  apagar(CHAVE_USUARIO);
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
