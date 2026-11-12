/**
 * Cliente da API Meus Direitos (contrato em docs/api.md).
 * Usa apenas fetch. Envia o token da sessão (Authorization: Bearer) e transforma qualquer erro no mesmo formato.
 */
import { lerToken, encerrarSessao } from './sessao.js';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

/** Erro devolvido pela API, já com status, mensagem e erros por campo. */
export class ErroApi extends Error {
  constructor(corpo) {
    super(corpo.mensagem ?? 'Algo deu errado. Tente novamente.');
    this.status = corpo.status ?? 0;
    this.campos = corpo.campos ?? []; // [{ campo: 'email', mensagem: '...' }]
  }

  /** { email: 'E-mail em formato inválido.', ... } para mostrar ao lado de cada campo */
  porCampo() {
    return Object.fromEntries(this.campos.map((c) => [c.campo, c.mensagem]));
  }
}

export async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const cabecalhos = { 'Content-Type': 'application/json' };
  const token = lerToken();
  if (token) cabecalhos.Authorization = `Bearer ${token}`;

  let resposta;
  try {
    resposta = await fetch(API_URL + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi({ status: 0, mensagem: 'Sem conexão no momento. Verifique sua internet e tente de novo.' });
  }

  if (resposta.status === 204) return null; // sucesso sem corpo (DELETE)

  let dados = null;
  try {
    dados = await resposta.json();
  } catch {
    // resposta sem JSON (ex.: erro do servidor antes de chegar à API)
  }

  if (!resposta.ok) {
    // token ausente, inválido ou vencido (7 dias): a sessão acabou
    if (resposta.status === 401) encerrarSessao();
    throw new ErroApi(dados ?? { status: resposta.status, mensagem: 'Algo deu errado. Tente novamente.' });
  }
  return dados;
}

/** Monta a query string ignorando valores vazios: query({ ordem: 'RECENTES', pagina: 2 }) → "?ordem=RECENTES&pagina=2" */
export function query(parametros) {
  const busca = new URLSearchParams();
  Object.entries(parametros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') busca.set(chave, valor);
  });
  const texto = busca.toString();
  return texto ? `?${texto}` : '';
}
