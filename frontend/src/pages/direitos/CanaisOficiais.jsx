import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import './direitos.css';

/**
 * Canais da tela 32. Conteúdo fixo do Figma (docs/api.md §10).
 * `link` fica vazio de propósito: o Figma não indica endereços e não inventamos URLs.
 * Quando o grupo definir um endereço oficial, basta preenchê-lo e o card vira link externo.
 */
const CANAIS = [
  { titulo: 'Portal da Prefeitura', texto: 'Serviços e benefícios municipais', link: '' },
  { titulo: 'Portal Gov.br', texto: 'Programas e serviços federais', link: '' },
  { titulo: 'Atendimento presencial', texto: 'Busque o CRAS ou órgão responsável na sua região.', link: '' },
];

/** 32 · Direitos / Canais oficiais */
export default function CanaisOficiais() {
  return (
    <Tela titulo="Canais de consulta" comNavegacao>
      <Cabecalho titulo="Canais de consulta" voltarPara="/direitos" />

      <p className="md-sobretitulo">Próximo passo</p>
      <h2 className="md-titulo">Confirme na fonte oficial.</h2>
      <p className="md-corpo">Escolha um canal para pesquisar os critérios.</p>

      <ul className="md-direito-canais list-unstyled m-0">
        {CANAIS.map((canal) => (
          <li key={canal.titulo}>
            <CardCanal {...canal} />
          </li>
        ))}
      </ul>

      <p className="md-legenda">Os links abrem sites externos. Nenhum pedido é enviado por este protótipo.</p>

      <Link to="/direitos" className="md-botao md-botao-secundario">
        Voltar aos direitos
      </Link>
    </Tela>
  );
}

function CardCanal({ titulo, texto, link }) {
  const conteudo = (
    <>
      <h3 className="md-subtitulo">{titulo}</h3>
      <p className="md-corpo">{texto}</p>
    </>
  );

  if (!link) return <div className="md-card">{conteudo}</div>;

  return (
    <a href={link} target="_blank" rel="noopener noreferrer" className="md-card md-direito-card">
      {conteudo}
      <span className="md-oculto-visualmente">(abre em nova aba)</span>
    </a>
  );
}
