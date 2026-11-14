import { Navigate, Route, Routes } from 'react-router-dom';
import RotaProtegida, { RotaPublica } from './components/RotaProtegida.jsx';
import GerenciarRota from './components/GerenciarRota.jsx';
import PainelApresentacao from './components/PainelApresentacao.jsx';

import BoasVindas from './pages/inicio/BoasVindas.jsx';
import Cadastro from './pages/inicio/Cadastro.jsx';
import Entrar from './pages/inicio/Entrar.jsx';
import Feed from './pages/cidade/Feed.jsx';
import DetalheDenuncia from './pages/cidade/DetalheDenuncia.jsx';
import PainelCidade from './pages/cidade/PainelCidade.jsx';
import NovaDenuncia from './pages/denuncias/NovaDenuncia.jsx';
import MinhasDenuncias from './pages/denuncias/MinhasDenuncias.jsx';
import Perfil from './pages/perfil/Perfil.jsx';
import EditarPerfil from './pages/perfil/EditarPerfil.jsx';
import TrocarSenha from './pages/perfil/TrocarSenha.jsx';
import ExcluirConta from './pages/perfil/ExcluirConta.jsx';
import MeusDireitos from './pages/direitos/MeusDireitos.jsx';
import Questionario from './pages/direitos/Questionario.jsx';
import DetalheDireito from './pages/direitos/DetalheDireito.jsx';
import CanaisOficiais from './pages/direitos/CanaisOficiais.jsx';
import TodosOsTemas from './pages/direitos/TodosOsTemas.jsx';

/** Mapa de telas (os números são os do Figma). */
export default function App() {
  return (
    <div className="md-moldura">
      <PainelApresentacao />
      <div className="md-app">
        <GerenciarRota />
        <Routes>
          {/* 01 · Bem-vindo */}
          <Route element={<RotaPublica />}>
            <Route path="/" element={<BoasVindas />} />
            <Route path="/cadastro" element={<Cadastro />} />
            <Route path="/entrar" element={<Entrar />} />
          </Route>

          <Route element={<RotaProtegida />}>
            {/* 02 · Sua cidade */}
            <Route path="/cidade" element={<Feed />} />
            <Route path="/cidade/painel" element={<PainelCidade />} />
            <Route path="/denuncias/:id" element={<DetalheDenuncia />} />

            {/* 03 · Participe */}
            <Route path="/denuncias/nova" element={<NovaDenuncia />} />
            <Route path="/denuncias/:id/editar" element={<NovaDenuncia />} />
            <Route path="/minhas" element={<MinhasDenuncias />} />

            {/* 04 · Sua conta */}
            <Route path="/perfil" element={<Perfil />} />
            <Route path="/perfil/editar" element={<EditarPerfil />} />
            <Route path="/perfil/senha" element={<TrocarSenha />} />
            <Route path="/perfil/excluir" element={<ExcluirConta />} />

            {/* 05 · Sua história e 06 · Seus direitos */}
            <Route path="/direitos" element={<MeusDireitos />} />
            <Route path="/direitos/questionario" element={<Questionario />} />
            <Route path="/direitos/canais" element={<CanaisOficiais />} />
            <Route path="/direitos/temas" element={<TodosOsTemas />} />
            <Route path="/direitos/:id" element={<DetalheDireito />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </div>
  );
}
