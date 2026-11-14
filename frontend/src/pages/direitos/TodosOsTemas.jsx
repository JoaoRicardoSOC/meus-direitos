import { useCallback, useEffect, useId, useMemo, useState } from 'react';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { CardTema } from './MeusDireitos.jsx';
import './direitos.css';

/** Tag que vale para qualquer pessoa: o tema aparece em todos os filtros, como no back-end. */
const TAG_TODOS = 'TODOS';

/**
 * Todos os temas (GET /direitos): o catálogo completo, para quem pulou o questionário ou quer ver
 * além do próprio perfil. O filtro por público roda aqui mesmo, sobre a lista já carregada
 * (são poucos temas), por isso a troca é instantânea e não depende da rede.
 */
export default function TodosOsTemas() {
  const id = useId();
  const [estado, setEstado] = useState({ carregando: true, erro: '', temas: [] });
  const [publico, setPublico] = useState('');

  const carregar = useCallback(async () => {
    setEstado({ carregando: true, erro: '', temas: [] });
    try {
      const temas = await api('/direitos');
      setEstado({ carregando: false, erro: '', temas: Array.isArray(temas) ? temas : [] });
    } catch (e) {
      setEstado({ carregando: false, erro: e.message, temas: [] });
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  // Opções do filtro: os públicos que aparecem no catálogo, sem o "todas as pessoas"
  const publicos = useMemo(() => {
    const porCodigo = new Map();
    estado.temas.forEach((tema) =>
      (tema.tags ?? []).forEach((tag) => {
        if (tag.codigo !== TAG_TODOS) porCodigo.set(tag.codigo, tag.descricao);
      }),
    );
    return [...porCodigo].map(([codigo, descricao]) => ({ codigo, descricao }));
  }, [estado.temas]);

  const visiveis = publico
    ? estado.temas.filter((tema) => (tema.tags ?? []).some((t) => t.codigo === publico || t.codigo === TAG_TODOS))
    : estado.temas;

  return (
    <Tela titulo="Todos os temas" comNavegacao gap={14}>
      <Cabecalho titulo="Todos os temas" voltarPara="/direitos" />

      <p className="md-corpo">Conheça os temas de direitos que a plataforma explica, em linguagem simples.</p>

      {estado.carregando && (
        <div className="md-direito-lista" role="status" aria-label="Carregando os temas">
          <span className="md-esqueleto md-direito-esqueleto-card" />
          <span className="md-esqueleto md-direito-esqueleto-card" />
          <span className="md-esqueleto md-direito-esqueleto-card" />
        </div>
      )}

      {estado.erro && (
        <>
          <Mensagem>{estado.erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={carregar}>
            Tentar novamente
          </button>
        </>
      )}

      {!estado.carregando && !estado.erro && (
        <>
          {publicos.length > 0 && (
            <div className="md-direito-filtro">
              <label htmlFor={`${id}-publico`} className="md-direito-filtro-rotulo">
                Para quem
              </label>
              <select
                id={`${id}-publico`}
                className="form-select md-entrada"
                value={publico}
                onChange={(e) => setPublico(e.target.value)}
              >
                <option value="">Todos os públicos</option>
                {publicos.map((p) => (
                  <option key={p.codigo} value={p.codigo}>
                    {p.descricao}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Anuncia o resultado do filtro para quem usa leitor de tela */}
          <p className="md-legenda" role="status">
            {visiveis.length === 1 ? '1 tema' : `${visiveis.length} temas`}
          </p>

          <div className="md-direito-lista md-grupo">
            {visiveis.length > 0 ? (
              <ul className="md-direito-lista list-unstyled m-0">
                {visiveis.map((tema) => (
                  <li key={tema.id}>
                    <CardTema direito={tema} voltarPara="/direitos/temas" />
                  </li>
                ))}
              </ul>
            ) : (
              <Mensagem tipo="info">Nenhum tema para este público ainda.</Mensagem>
            )}
            <p className="md-legenda">Esta lista orienta a pesquisa. Não confirma elegibilidade.</p>
          </div>
        </>
      )}
    </Tela>
  );
}
