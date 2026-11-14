import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import { Toast } from '../../components/Mensagem.jsx';
import iconeEscudo from '../../assets/icones/escudo-destaque.svg';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import './PrimeiroAcesso.css';

/**
 * 22 · Direitos / Primeiro acesso.
 * Mostrada em "Meus Direitos" enquanto a pessoa não respondeu o questionário (GET /usuarios/perfil → 404).
 * É a raiz da aba: sem seta de voltar, com o título da aba como na tela de direitos.
 * `aviso` (opcional) aparece num aviso temporário, ex.: depois de apagar as respostas.
 */
export default function PrimeiroAcesso({ aviso, aoFecharAviso }) {
  return (
    <Tela titulo="Meus Direitos" comNavegacao>
      <h1 className="md-titulo" tabIndex={-1} data-foco-tela>
        Meus Direitos
      </h1>

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

      {/* Aviso de privacidade: o cadeado fica só aqui */}
      <div className="md-informacao">
        <img src={iconeCadeado} alt="" width="20" height="20" />
        <p className="md-legenda">
          Você escolhe o que informar. Dados sensíveis são opcionais. Neste protótipo, as respostas são fictícias.
        </p>
      </div>

      <div className="md-primeiro-acesso-acoes md-grupo">
        <Link to="/direitos/questionario" className="md-botao md-botao-primario">
          Conhecer meus direitos
        </Link>
        <Link to="/cidade" className="md-botao md-botao-secundario">
          Agora não
        </Link>
        <Link to="/direitos/temas" className="md-acao-texto md-direito-ver-todos">
          Ver todos os temas sem responder
        </Link>
      </div>

      <Toast texto={aviso} chave={aviso} aoFechar={aoFecharAviso} />
    </Tela>
  );
}
