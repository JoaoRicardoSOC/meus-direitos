import { useEffect } from 'react';
import NavegacaoPrincipal from './NavegacaoPrincipal.jsx';

/**
 * Moldura de cada tela: margem de 24 px, espaçamento vertical e, nas telas logadas,
 * a navegação principal fixa embaixo. Também atualiza o título da aba do navegador.
 */
export default function Tela({ titulo, comNavegacao = false, gap, className = '', children }) {
  useEffect(() => {
    document.title = titulo ? `${titulo} · Meus Direitos` : 'Meus Direitos';
  }, [titulo]);

  return (
    <>
      <main className={`md-tela ${comNavegacao ? 'md-com-navegacao' : ''} ${className}`} style={gap ? { gap } : undefined}>
        {children}
      </main>
      {comNavegacao && <NavegacaoPrincipal />}
    </>
  );
}
