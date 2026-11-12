import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { aoMudarSessao, encerrarSessao, iniciarSessao, lerUsuario, lerUsuarioId } from '../services/sessao.js';

const SessaoContext = createContext(null);

function lerEstado() {
  return lerUsuarioId() ? lerUsuario() : null;
}

/** Deixa o usuário logado disponível para todas as telas (useSessao). */
export function SessaoProvider({ children }) {
  const [usuario, setUsuario] = useState(lerEstado);

  // login, logout e respostas 401 (que apagam a sessão no cliente da API)
  useEffect(() => aoMudarSessao(() => setUsuario(lerEstado())), []);

  const entrar = useCallback((dados) => iniciarSessao(dados), []);
  const sair = useCallback(() => encerrarSessao(), []);

  const valor = useMemo(() => ({ usuario, logado: Boolean(usuario), entrar, sair }), [usuario, entrar, sair]);
  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>;
}

/** { usuario, logado, entrar(usuario), sair() } */
export function useSessao() {
  const contexto = useContext(SessaoContext);
  if (!contexto) throw new Error('useSessao precisa estar dentro de <SessaoProvider>.');
  return contexto;
}
