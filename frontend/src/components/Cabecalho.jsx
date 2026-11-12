import { useNavigate } from 'react-router-dom';
import iconeVoltar from '../assets/icones/arrow.svg';

/**
 * Cabeçalho com o botão "Voltar" (seta) e o título da tela.
 * Sem `voltarPara`, volta para a tela anterior do histórico.
 */
export default function Cabecalho({ titulo, voltarPara, aoVoltar }) {
  const navegar = useNavigate();

  function voltar() {
    if (aoVoltar) aoVoltar();
    else if (voltarPara) navegar(voltarPara);
    else navegar(-1);
  }

  return (
    <header className="md-cabecalho">
      <button type="button" className="md-voltar" onClick={voltar} aria-label="Voltar">
        <img src={iconeVoltar} alt="" width="24" height="24" />
      </button>
      <h1 className="md-subtitulo">{titulo}</h1>
    </header>
  );
}
