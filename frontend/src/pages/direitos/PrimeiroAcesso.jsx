import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import iconeEscudo from '../../assets/icones/escudo-destaque.svg';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import './PrimeiroAcesso.css';

/**
 * 22 · Direitos / Primeiro acesso.
 * Mostrada em "Meus Direitos" enquanto a pessoa não respondeu o questionário (GET /usuarios/perfil → 404).
 */
export default function PrimeiroAcesso() {
  return (
    <Tela titulo="Meus Direitos" comNavegacao>
      <Cabecalho titulo="Meus Direitos" voltarPara="/cidade" />
      <div className="md-primeiro-acesso-espaco" />

      <section className="md-primeiro-acesso-destaque" aria-labelledby="primeiro-acesso-titulo">
        <img src={iconeEscudo} alt="" width="44" height="44" />
        <h2 id="primeiro-acesso-titulo" className="md-titulo">
          Informação que
          <br />
          abre caminhos.
        </h2>
        <p className="md-corpo">Descubra possibilidades de apoio para o seu momento de vida.</p>
      </section>

      <p className="md-corpo">
        Para encontrar benefícios que podem se aplicar a você, precisamos conhecer um pouco mais sobre sua
        realidade. É rápido.
      </p>

      <div className="md-informacao">
        <img src={iconeCadeado} alt="" width="20" height="20" />
        <p className="md-legenda">
          Você escolhe o que informar. Dados sensíveis são opcionais. Neste protótipo, as respostas são fictícias.
        </p>
      </div>

      <Link to="/direitos/questionario" className="md-botao md-botao-primario">
        Conhecer meus direitos
      </Link>
      <Link to="/cidade" className="md-botao md-botao-secundario">
        Agora não
      </Link>
    </Tela>
  );
}
