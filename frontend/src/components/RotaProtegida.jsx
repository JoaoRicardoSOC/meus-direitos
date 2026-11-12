import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useSessao } from '../contexts/SessaoContext.jsx';

/** Telas que exigem login: sem sessão (ou depois de um 401), volta para "Entrar". */
export default function RotaProtegida() {
  const { logado } = useSessao();
  const local = useLocation();
  if (!logado) {
    return <Navigate to="/entrar" replace state={{ voltarPara: local.pathname }} />;
  }
  return <Outlet />;
}

/** Telas só para quem não entrou (boas-vindas, cadastro, login): logado vai direto para a cidade. */
export function RotaPublica() {
  const { logado } = useSessao();
  return logado ? <Navigate to="/cidade" replace /> : <Outlet />;
}
