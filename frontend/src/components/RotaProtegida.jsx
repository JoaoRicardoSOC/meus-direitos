import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useSessao } from '../contexts/SessaoContext.jsx';

/**
 * Telas que exigem login. Sem sessão: depois de "Sair da conta" ou de excluir a conta, vai para as
 * boas-vindas; se a sessão caiu (resposta 401) ou a pessoa abriu um link sem login, vai para "Entrar"
 * e volta à tela pedida depois do login.
 */
export default function RotaProtegida() {
  const { logado, saidaVoluntaria } = useSessao();
  const local = useLocation();
  if (!logado) {
    if (saidaVoluntaria.current) return <Navigate to="/" replace />;
    return <Navigate to="/entrar" replace state={{ voltarPara: local.pathname }} />;
  }
  return <Outlet />;
}

/** Telas só para quem não entrou (boas-vindas, cadastro, login): logado vai direto para a cidade. */
export function RotaPublica() {
  const { logado } = useSessao();
  return logado ? <Navigate to="/cidade" replace /> : <Outlet />;
}
