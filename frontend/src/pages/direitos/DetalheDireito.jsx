import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import './direitos.css';

/**
 * 29 · Direito / Transporte, 30 · Direito / Prioridade e 31 · Direito / Município (GET /direitos/{id}).
 * As três telas do Figma são o mesmo layout com conteúdos diferentes: tudo aqui vem da API.
 * Aviso, base legal e link oficial só aparecem quando o tema os tiver.
 */
export default function DetalheDireito() {
  const { id } = useParams();
  const [estado, setEstado] = useState({ carregando: true, erro: '', naoEncontrado: false, direito: null });

  const carregar = useCallback(async () => {
    setEstado({ carregando: true, erro: '', naoEncontrado: false, direito: null });
    try {
      const direito = await api(`/direitos/${encodeURIComponent(id)}`);
      setEstado({ carregando: false, erro: '', naoEncontrado: false, direito });
    } catch (e) {
      setEstado({ carregando: false, erro: e.message, naoEncontrado: e.status === 404 || e.status === 400, direito: null });
    }
  }, [id]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const { direito } = estado;
  const passos = [...(direito?.passos ?? [])].sort((a, b) => a.ordem - b.ordem);

  return (
    <Tela titulo={direito?.titulo ?? 'Direito'} comNavegacao>
      <Cabecalho titulo="Meus Direitos" voltarPara="/direitos" />

      {estado.carregando && <Carregando texto="Carregando o tema…" />}

      {estado.naoEncontrado && (
        <>
          <Mensagem>Não encontramos este tema. Ele pode ter sido removido do catálogo.</Mensagem>
          <Link to="/direitos" className="md-botao md-botao-primario">
            Voltar aos direitos
          </Link>
        </>
      )}

      {estado.erro && !estado.naoEncontrado && (
        <>
          <Mensagem>{estado.erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={carregar}>
            Tentar novamente
          </button>
          <Link to="/direitos" className="md-botao md-botao-secundario">
            Voltar aos direitos
          </Link>
        </>
      )}

      {direito && (
        <>
          <p className="md-sobretitulo">Possibilidade a verificar</p>
          <h2 className="md-titulo">{direito.titulo}</h2>
          {(direito.descricao ?? direito.resumo) && <p className="md-corpo">{direito.descricao ?? direito.resumo}</p>}

          {passos.length > 0 && (
            <section className="md-card" aria-labelledby="titulo-passos">
              <h3 id="titulo-passos" className="md-subtitulo">
                Por onde começar
              </h3>
              <ol className="md-direito-passos">
                {passos.map((passo) => (
                  <li key={passo.ordem}>
                    <span className="md-negrito md-direito-destaque">
                      {String(passo.ordem).padStart(2, '0')}
                      {'  '}
                      {passo.titulo}
                    </span>
                    {passo.texto && <span className="md-corpo">{passo.texto}</span>}
                  </li>
                ))}
              </ol>
            </section>
          )}

          {direito.aviso && (
            <div className="md-informacao">
              <img src={iconeCadeado} alt="" width="20" height="20" />
              <p className="md-legenda">{direito.aviso}</p>
            </div>
          )}

          {direito.baseLegal && (
            <p className="md-legenda">
              <strong>Base legal:</strong> {direito.baseLegal}
            </p>
          )}

          {direito.linkOficial && (
            <a href={direito.linkOficial} target="_blank" rel="noopener noreferrer" className="md-negrito md-direito-destaque">
              Ver a fonte oficial deste tema
              <span className="md-oculto-visualmente"> (abre em nova aba)</span>
            </a>
          )}

          <Link to="/direitos/canais" className="md-botao md-botao-primario">
            Consultar canais oficiais
          </Link>
          <Link to="/direitos" className="md-botao md-botao-secundario">
            Voltar aos direitos
          </Link>
        </>
      )}
    </Tela>
  );
}
