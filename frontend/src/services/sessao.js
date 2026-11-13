/**
 * Sessão do usuário (docs/api.md, seção 4). Depois do cadastro ou login guardamos o token
 * (enviado no cabeçalho Authorization: Bearer) e, separado dele, o usuário.
 */
const CHAVE_TOKEN = 'token';
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

export function lerToken() {
  return ler(CHAVE_TOKEN);
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

/**
 * Guarda o usuário devolvido por POST /usuarios, POST /auth/login, GET ou PUT /usuarios/me.
 * O token só vem no cadastro e no login: se não vier, o token já guardado continua valendo.
 * O usuário é salvo sem o token.
 */
export function iniciarSessao({ token, ...usuario }) {
  if (token) gravar(CHAVE_TOKEN, token);
  gravar(CHAVE_USUARIO, JSON.stringify(usuario));
  window.dispatchEvent(new Event(EVENTO));
}

/** "Sair da conta", conta excluída ou resposta 401. */
export function encerrarSessao() {
  apagar(CHAVE_TOKEN);
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
