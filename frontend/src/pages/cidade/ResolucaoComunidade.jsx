import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';

/** "São Paulo" e "sao paulo" são a mesma cidade: compara sem acento e sem maiúsculas. */
function normalizar(texto) {
  return (texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toLowerCase();
}

/**
 * Resolução pela comunidade (seção 8.7 do guia, sem tela no Figma).
 * Mostra "X de 3 moradores confirmaram que foi resolvido" e o botão "Já foi resolvido" /
 * "Retirar confirmação" para moradores da mesma cidade que não são o autor.
 */
export default function ResolucaoComunidade({ denuncia, aoAtualizar }) {
  const { usuario } = useSessao();
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');

  const { confirmacoes = 0, necessarias = 3, euConfirmei = false } = denuncia.resolucao ?? {};
  const resolvida = denuncia.status?.codigo === 'RESOLVIDA';
  const ehAutor = usuario && denuncia.autor.id === usuario.id;
  const mesmaCidade =
    usuario && normalizar(usuario.cidade) === normalizar(denuncia.cidade) && normalizar(usuario.uf) === normalizar(denuncia.uf);
  const podeConfirmar = usuario && !ehAutor && mesmaCidade && !resolvida;

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

  return (
    <section className="md-card" aria-labelledby="resolucao-titulo">
      <h2 id="resolucao-titulo" className="md-sobretitulo">
        {resolvida ? 'Problema resolvido' : 'Já foi resolvido?'}
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

      {resolvida && <p className="md-corpo">Esta denúncia foi marcada como resolvida.</p>}

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

      {ehAutor && (
        <p className="md-legenda">
          {resolvida ? 'Precisa reabrir? ' : 'O problema acabou? '}
          Acesse <Link to="/minhas">Minhas denúncias</Link> para {resolvida ? 'reabrir' : 'marcar como resolvida'}.
        </p>
      )}

      {erro && (
        <p className="md-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </section>
  );
}
