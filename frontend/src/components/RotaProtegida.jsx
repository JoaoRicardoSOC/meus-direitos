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

/**
 * Telas só para quem não entrou (boas-vindas, cadastro, login): logado vai para a tela pedida antes do
 * login (state.voltarPara, posto pela RotaProtegida) ou para a cidade.
 * Ao entrar, esta rota e o "Entrar" navegam quase ao mesmo tempo; os dois usam o mesmo destino, senão
 * quem chegasse primeiro decidia (e a pessoa caía na cidade em vez da tela que tinha aberto).
 */
export function RotaPublica() {
  const { logado } = useSessao();
  const local = useLocation();
  return logado ? <Navigate to={local.state?.voltarPara ?? '/cidade'} replace /> : <Outlet />;
}
