import { NavLink } from 'react-router-dom';
import iconeCidade from '../assets/icones/home.svg';
import iconeDireitos from '../assets/icones/shield.svg';
import iconePerfil from '../assets/icones/user.svg';

const ITENS = [
  { para: '/cidade', rotulo: 'Cidade', icone: iconeCidade },
  { para: '/direitos', rotulo: 'Meus Direitos', icone: iconeDireitos },
  { para: '/perfil', rotulo: 'Perfil', icone: iconePerfil },
];

/** Barra inferior "Cidade · Meus Direitos · Perfil", o mesmo menu em todas as telas logadas. */
export default function NavegacaoPrincipal() {
  return (
    <nav className="md-navegacao" aria-label="Navegação principal">
      {ITENS.map((item) => (
        <NavLink key={item.para} to={item.para} className="md-navegacao-item">
          <img src={item.icone} alt="" width="22" height="22" />
          {item.rotulo}
        </NavLink>
      ))}
    </nav>
  );
}
