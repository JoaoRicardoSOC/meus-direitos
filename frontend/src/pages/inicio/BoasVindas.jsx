import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import marca from '../../assets/icones/marca.svg';
import ilustracao from '../../assets/imagens/comunidade.svg';
import './inicio.css';

/** 01 · Boas-vindas */
export default function BoasVindas() {
  return (
    <Tela gap={24}>
      <div className="d-flex align-items-center gap-2 md-marca">
        <img src={marca} alt="" width="32" height="32" />
        <span className="md-subtitulo md-marca-nome">meus direitos</span>
      </div>

      <div className="md-ilustracao" aria-hidden="true">
        <img src={ilustracao} alt="" width="342" height="200" />
      </div>

      <p className="md-sobretitulo">Uma cidade para todos</p>
      <h1 className="md-titulo">
        Bem-vindo ao
        <br />
        Meus Direitos
      </h1>
      <p className="md-corpo">
        Sua voz melhora a cidade.
        <br />
        Seus direitos abrem caminhos.
      </p>

      <Link to="/cadastro" className="md-botao md-botao-primario">
        Começar
      </Link>
      <Link to="/entrar" className="md-botao md-botao-secundario">
        Já tenho uma conta
      </Link>

      <p className="md-legenda">Protótipo acadêmico · GS 2026</p>
    </Tela>
  );
}
