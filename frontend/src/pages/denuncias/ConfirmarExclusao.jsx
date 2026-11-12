import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';

/**
 * 18 · Excluir denúncia: confirmação antes do DELETE /denuncias/{id}.
 */
export default function ConfirmarExclusao({ denuncia, excluindo, erro, aoConfirmar, aoCancelar }) {
  return (
    <Tela titulo="Excluir denúncia" gap={12}>
      <Cabecalho titulo="Gerenciar denúncia" aoVoltar={aoCancelar} />
      <div className="md-espaco-exclusao" />
      <p className="md-sobretitulo">Confirmação</p>
      <h2 className="md-titulo">Excluir esta denúncia?</h2>
      <p className="md-corpo">Ela deixará de aparecer no feed. Esta ação é apenas uma simulação.</p>
      <p className="md-legenda">“{denuncia.titulo}” · protocolo #{denuncia.protocolo}</p>
      <Mensagem>{erro}</Mensagem>
      <button type="button" className="md-botao md-botao-perigo mt-2" onClick={aoConfirmar} disabled={excluindo}>
        {excluindo ? 'Excluindo…' : 'Sim, excluir'}
      </button>
      <button type="button" className="md-botao md-botao-secundario" onClick={aoCancelar} disabled={excluindo}>
        Manter denúncia
      </button>
    </Tela>
  );
}
