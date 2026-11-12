import { Link } from 'react-router-dom';
import iconeMais from '../assets/icones/plus.svg';

/** Botão flutuante "Nova denúncia" (FAB), acima da navegação principal. */
export default function BotaoNovaDenuncia() {
  return (
    <Link to="/denuncias/nova" className="md-fab">
      <img src={iconeMais} alt="" width="20" height="20" />
      Nova denúncia
    </Link>
  );
}
