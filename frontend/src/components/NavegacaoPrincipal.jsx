import { Link, useLocation } from 'react-router-dom';
import iconeCidade from '../assets/icones/home.svg';
import iconeDireitos from '../assets/icones/shield.svg';
import iconePerfil from '../assets/icones/user.svg';

/**
 * Cada aba fica ativa em todas as telas da sua seção, não só na raiz:
 * o detalhe de uma denúncia é da Cidade, "Minhas denúncias" é do Perfil (é aberta por lá).
 */
const ITENS = [
  { para: '/cidade', rotulo: 'Cidade', icone: iconeCidade, prefixos: ['/cidade', '/denuncias'] },
  { para: '/direitos', rotulo: 'Meus Direitos', icone: iconeDireitos, prefixos: ['/direitos'] },
  { para: '/perfil', rotulo: 'Perfil', icone: iconePerfil, prefixos: ['/perfil', '/minhas'] },
];

/** Barra inferior "Cidade · Meus Direitos · Perfil", o mesmo menu em todas as telas logadas. */
export default function NavegacaoPrincipal() {
  const { pathname } = useLocation();

  return (
    <nav className="md-navegacao" aria-label="Navegação principal">
      {ITENS.map((item) => {
        const ativo = item.prefixos.some((prefixo) => pathname === prefixo || pathname.startsWith(`${prefixo}/`));
        return (
          <Link
            key={item.para}
            to={item.para}
            className={`md-navegacao-item ${ativo ? 'active' : ''}`}
            aria-current={ativo ? 'page' : undefined}
          >
            <img src={item.icone} alt="" width="22" height="22" />
            {item.rotulo}
          </Link>
        );
      })}
    </nav>
  );
}
