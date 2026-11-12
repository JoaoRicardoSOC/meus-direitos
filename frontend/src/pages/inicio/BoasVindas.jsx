import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import marca from '../../assets/icones/marca.svg';
import ilustracao from '../../assets/imagens/comunidade.svg';
import './inicio.css';

/** 01 · Boas-vindas: o conteúdo ocupa a altura útil e as ações ficam embaixo, perto do polegar. */
export default function BoasVindas() {
  return (
    <Tela gap={24} className="md-boas-vindas">
      <div className="d-flex align-items-center gap-2 md-marca">
        <img src={marca} alt="" width="32" height="32" />
        <span className="md-subtitulo md-marca-nome">meus direitos</span>
      </div>

      <div className="md-ilustracao" aria-hidden="true">
        <img src={ilustracao} alt="" width="342" height="200" />
      </div>

      <h1 className="md-titulo" tabIndex={-1} data-foco-tela>
        Bem-vindo ao
        <br />
        Meus Direitos
      </h1>
      <p className="md-corpo">
        Sua voz melhora a cidade.
        <br />
        Seus direitos abrem caminhos.
      </p>

      <div className="md-boas-vindas-acoes">
        <Link to="/cadastro" className="md-botao md-botao-primario">
          Começar
        </Link>
        <Link to="/entrar" className="md-botao md-botao-secundario">
          Já tenho uma conta
        </Link>
        <p className="md-legenda text-center">Protótipo acadêmico · GS 2026</p>
      </div>
    </Tela>
  );
}
