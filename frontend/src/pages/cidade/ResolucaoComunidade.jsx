import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { chaveTexto } from '../../utils/formatos.js';

/**
 * Resolução pela comunidade (seção 8.7 do guia, sem tela no Figma), logo abaixo do card no detalhe.
 * Mostra "X de 3 moradores confirmaram que foi resolvido" e o botão "Já foi resolvido" /
 * "Retirar confirmação" para moradores da mesma cidade que não são o autor. Quem não pode
 * confirmar vê o porquê.
 */
export default function ResolucaoComunidade({ denuncia, aoAtualizar }) {
  const { usuario } = useSessao();
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  const { confirmacoes = 0, necessarias = 3, euConfirmei = false } = denuncia.resolucao ?? {};
  const resolvida = denuncia.status?.codigo === 'RESOLVIDA';
  const ehAutor = Boolean(usuario) && denuncia.autor.id === usuario.id;
  const mesmaCidade =
    Boolean(usuario) &&
    chaveTexto(usuario.cidade) === chaveTexto(denuncia.cidade) &&
    chaveTexto(usuario.uf) === chaveTexto(denuncia.uf);
  const podeConfirmar = Boolean(usuario) && !ehAutor && mesmaCidade && !resolvida;

  async function alternarConfirmacao() {
    setEnviando(true);
    setErro('');
    try {
      const atualizada = await api(`/denuncias/${denuncia.id}/confirmacao`, { metodo: euConfirmei ? 'DELETE' : 'PUT' });
      aoAtualizar(atualizada);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  let porque = null;
  if (resolvida) {
    porque = confirmacoes >= necessarias ? 'Resolvida pela comunidade.' : 'Esta denúncia foi marcada como resolvida.';
  } else if (!usuario) {
    porque = 'Entre na sua conta para confirmar.';
  } else if (ehAutor) {
    porque = (
      <>
        Você registrou esta denúncia: marque como resolvida em <Link to="/minhas">Minhas denúncias</Link>.
      </>
    );
  } else if (!mesmaCidade) {
    porque = `Só moradores de ${denuncia.cidade} podem confirmar.`;
  }

  return (
    <section className="md-card" aria-labelledby="resolucao-titulo">
      <h2 id="resolucao-titulo" className="md-negrito">
        Já foi resolvido?
      </h2>

      {!resolvida && (
        <>
          <div className="md-progresso md-progresso-resolucao" aria-hidden="true">
            {Array.from({ length: necessarias }, (_, i) => (
              <span key={i} className={`md-progresso-etapa ${i < confirmacoes ? 'ativa' : ''}`} />
            ))}
          </div>
          <p className="md-legenda" role="status">
            {confirmacoes} de {necessarias} moradores confirmaram que foi resolvido
            {euConfirmei ? ' (você confirmou)' : ''}
          </p>
        </>
      )}

      {porque && <p className={resolvida ? 'md-corpo-tinta' : 'md-legenda'}>{porque}</p>}

      {resolvida && ehAutor && (
        <p className="md-legenda">
          Precisa reabrir? Acesse <Link to="/minhas">Minhas denúncias</Link>.
        </p>
      )}

      {podeConfirmar && (
        <button
          type="button"
          className={`md-botao ${euConfirmei ? 'md-botao-secundario' : 'md-botao-primario'}`}
          onClick={alternarConfirmacao}
          disabled={enviando}
        >
          {enviando ? 'Enviando…' : euConfirmei ? 'Retirar confirmação' : 'Já foi resolvido'}
        </button>
      )}

      <Mensagem>{erro}</Mensagem>
    </section>
  );
}
