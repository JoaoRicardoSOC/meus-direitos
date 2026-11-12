import Tela from './Tela.jsx';

/** Marcador temporário de telas que ainda serão implementadas. */
export default function EmConstrucao({ titulo }) {
  return (
    <Tela titulo={titulo} comNavegacao>
      <h1 className="md-titulo">{titulo}</h1>
      <p className="md-corpo">Esta tela ainda está em construção.</p>
    </Tela>
  );
}
