import { useEffect, useRef } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';
import { focarTitulo } from '../utils/foco.js';

/**
 * A cada tela nova (PUSH/REPLACE): página no topo, na hora, e foco no título, para o leitor de tela
 * anunciar onde a pessoa está. No "voltar" (POP) a rolagem fica com a tela (o feed restaura a dele).
 * Muda só quando muda a rota ou a etapa (?passo=); âncoras (#conversa) ficam com a própria tela.
 */
export default function GerenciarRota() {
  const local = useLocation();
  const tipo = useNavigationType();
  const primeiraVez = useRef(true);

  useEffect(() => {
    if (primeiraVez.current) {
      primeiraVez.current = false;
      return; // ao abrir o app, o foco fica no começo da página, como de costume
    }
    if (tipo !== 'POP' && !local.hash) {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    }
    focarTitulo();
  }, [local.pathname, local.search]); // eslint-disable-line react-hooks/exhaustive-deps

  return null;
}
