import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { aoMudarSessao, encerrarSessao, iniciarSessao, lerToken, lerUsuario } from '../services/sessao.js';

const SessaoContext = createContext(null);

/** Logado = há token e usuário guardados. */
function lerEstado() {
  return lerToken() ? lerUsuario() : null;
}

/** Deixa o usuário logado disponível para todas as telas (useSessao). */
export function SessaoProvider({ children }) {
  const [usuario, setUsuario] = useState(lerEstado);
  /**
   * A pessoa saiu por vontade própria ("Sair da conta", conta excluída)? Então as telas protegidas
   * levam para as boas-vindas. Se a sessão caiu sozinha (resposta 401), levam para "Entrar".
   */
  const saidaVoluntaria = useRef(false);

  // login, logout e respostas 401 (que apagam a sessão no cliente da API)
  useEffect(() => aoMudarSessao(() => setUsuario(lerEstado())), []);

  const entrar = useCallback((dados) => {
    saidaVoluntaria.current = false;
    iniciarSessao(dados);
  }, []);
  const sair = useCallback(() => {
    saidaVoluntaria.current = true;
    encerrarSessao();
  }, []);

  const valor = useMemo(
    () => ({ usuario, logado: Boolean(usuario), entrar, sair, saidaVoluntaria }),
    [usuario, entrar, sair],
  );
  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>;
}

/** { usuario, logado, entrar(usuario), sair(), saidaVoluntaria (ref) } */
export function useSessao() {
  const contexto = useContext(SessaoContext);
  if (!contexto) throw new Error('useSessao precisa estar dentro de <SessaoProvider>.');
  return contexto;
}
