import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import iconeMais from '../assets/icones/plus.svg';

const DISTANCIA_MINIMA = 8; // px: ignora tremidas da rolagem

/**
 * Botão flutuante "Nova denúncia" (FAB), acima da navegação principal.
 * Enquanto a pessoa rola para baixo ele recolhe para só o "+" (não cobre os votos dos cards);
 * ao rolar para cima ou voltar ao topo, mostra o texto de novo.
 */
export default function BotaoNovaDenuncia() {
  const [recolhido, setRecolhido] = useState(false);

  useEffect(() => {
    let ultimo = window.scrollY;
    let agendado = false;

    function aoRolar() {
      if (agendado) return;
      agendado = true;
      requestAnimationFrame(() => {
        agendado = false;
        const atual = window.scrollY;
        if (atual < 80) setRecolhido(false);
        else if (atual - ultimo > DISTANCIA_MINIMA) setRecolhido(true);
        else if (ultimo - atual > DISTANCIA_MINIMA) setRecolhido(false);
        ultimo = atual;
      });
    }

    window.addEventListener('scroll', aoRolar, { passive: true });
    return () => window.removeEventListener('scroll', aoRolar);
  }, []);

  return (
    <Link to="/denuncias/nova" className={`md-fab ${recolhido ? 'recolhido' : ''}`} aria-label="Nova denúncia">
      <img src={iconeMais} alt="" width="20" height="20" />
      <span className="md-fab-texto" aria-hidden="true">
        Nova denúncia
      </span>
    </Link>
  );
}
