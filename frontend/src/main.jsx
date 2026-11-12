import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
// Do Bootstrap, só o reset e os utilitários de layout (d-flex, gap-*, m-*...): o resto vem do tema
import 'bootstrap/dist/css/bootstrap-reboot.min.css';
import 'bootstrap/dist/css/bootstrap-utilities.min.css';
import './styles/tema.css';
import { SessaoProvider } from './contexts/SessaoContext.jsx';
import App from './App.jsx';

// HashRouter: as rotas ficam depois do "#", então o index.html funciona aberto direto do disco
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <HashRouter>
      <SessaoProvider>
        <App />
      </SessaoProvider>
    </HashRouter>
  </StrictMode>,
);
