import { Link } from 'react-router-dom';
import { iniciais } from '../utils/formatos.js';

/** Círculo com as iniciais do nome ("MO"). Com `para`, vira link (ex.: avatar do topo do feed → Perfil). */
export default function Avatar({ nome, pequeno = false, para, rotulo }) {
  const classe = `md-avatar ${pequeno ? 'md-avatar-pequeno' : ''}`;
  if (para) {
    return (
      <Link to={para} className={classe} aria-label={rotulo ?? nome}>
        {iniciais(nome)}
      </Link>
    );
  }
  return (
    <span className={classe} aria-hidden="true">
      {iniciais(nome)}
    </span>
  );
}
