import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import iconeCheck from '../../assets/icones/check.svg';

/**
 * 16 · Denúncia publicada: confirmação com o `protocolo` devolvido pelo POST /denuncias.
 */
export default function DenunciaPublicada({ denuncia }) {
  return (
    <Tela titulo="Denúncia publicada" gap={20}>
      <div className="md-espaco-publicada" />
      <section className="md-confirmacao" aria-labelledby="titulo-publicada">
        <img src={iconeCheck} alt="" width="52" height="52" />
        <h1 id="titulo-publicada" className="md-titulo">Sua voz foi ouvida.</h1>
        <p className="md-corpo">
          Denúncia publicada
          <br />
          na comunidade.
        </p>
        <span className="md-selo">PROTOCOLO DEMO · #{denuncia.protocolo}</span>
      </section>
      <p className="md-corpo">
        Acompanhe os votos e os comentários na sua denúncia. O envio à prefeitura ainda não faz parte desta
        demonstração.
      </p>
      <Link to={`/denuncias/${denuncia.id}`} className="md-botao md-botao-primario">
        Ver minha denúncia
      </Link>
      <Link to="/cidade" className="md-botao md-botao-secundario">
        Voltar para a cidade
      </Link>
    </Tela>
  );
}
