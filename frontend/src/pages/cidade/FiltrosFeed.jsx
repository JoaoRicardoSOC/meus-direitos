import { useEffect, useId, useRef, useState } from 'react';
import { api } from '../../services/api.js';
import { CATEGORIAS_PADRAO } from '../denuncias/opcoes.js';

export const FILTROS_VAZIOS = { busca: '', categoria: '', status: '' };
export const TAMANHO_BUSCA = 100; // limite de `busca` na API (docs/api.md, 8.5)

const STATUS_PADRAO = [
  { codigo: 'ABERTA', descricao: 'Em aberto' },
  { codigo: 'EM_ANDAMENTO', descricao: 'Em andamento' },
  { codigo: 'RESOLVIDA', descricao: 'Resolvida' },
];

// opções dos <select> já recebidas: ao voltar para o feed não pedem de novo
let opcoesEmCache = null;

/** Há algum filtro aplicado? Sem nenhum, o feed mostra as abas como sempre. */
export function temFiltro(filtros) {
  return Boolean(filtros?.busca || filtros?.categoria || filtros?.status);
}

/**
 * Busca e filtros do feed: texto (aplicado ao enviar), categoria e situação (aplicadas ao escolher).
 * `filtros` é o que está aplicado; o texto digitado só vale depois de "Buscar".
 */
export default function FiltrosFeed({ filtros, aoFiltrar }) {
  const id = useId();
  const entrada = useRef(null);
  const [texto, setTexto] = useState(filtros.busca);
  // filtros limpos por fora (botão do resultado vazio): o campo acompanha
  const [buscaAplicada, setBuscaAplicada] = useState(filtros.busca);
  if (buscaAplicada !== filtros.busca) {
    setBuscaAplicada(filtros.busca);
    setTexto(filtros.busca);
  }
  const [opcoes, setOpcoes] = useState(
    () => opcoesEmCache ?? { categorias: CATEGORIAS_PADRAO, status: STATUS_PADRAO },
  );

  // Listas da API (GET /categorias e /status-denuncia); se falharem, ficam as documentadas
  useEffect(() => {
    if (opcoesEmCache) return undefined;
    let ativo = true;
    Promise.allSettled([api('/categorias'), api('/status-denuncia')]).then(([c, s]) => {
      const categorias = c.status === 'fulfilled' && Array.isArray(c.value) && c.value.length ? c.value : null;
      const status = s.status === 'fulfilled' && Array.isArray(s.value) && s.value.length ? s.value : null;
      const novas = { categorias: categorias ?? CATEGORIAS_PADRAO, status: status ?? STATUS_PADRAO };
      if (categorias && status) opcoesEmCache = novas;
      if (ativo) setOpcoes(novas);
    });
    return () => {
      ativo = false;
    };
  }, []);

  function buscar(evento) {
    evento.preventDefault();
    aoFiltrar({ ...filtros, busca: texto.trim() });
  }

  // escolher categoria ou situação já aplica, junto com o texto digitado
  function escolher(campo, valor) {
    aoFiltrar({ ...filtros, busca: texto.trim(), [campo]: valor });
  }

  function limpar() {
    setTexto('');
    aoFiltrar(FILTROS_VAZIOS);
    entrada.current?.focus();
  }

  const podeLimpar = temFiltro(filtros) || texto !== '';

  return (
    <form role="search" aria-label="Buscar denúncias da cidade" className="md-busca" onSubmit={buscar}>
      <label htmlFor={`${id}-busca`} className="md-oculto-visualmente">
        Buscar denúncias
      </label>
      <div className="md-busca-linha">
        <input
          ref={entrada}
          data-busca-feed
          id={`${id}-busca`}
          type="search"
          className="form-control md-entrada"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar denúncias"
          maxLength={TAMANHO_BUSCA}
          enterKeyHint="search"
          autoComplete="off"
        />
        <button type="submit" className="md-botao md-botao-secundario md-busca-botao">
          Buscar
        </button>
      </div>

      <div className="md-busca-filtros">
        <div className="md-busca-filtro">
          <label htmlFor={`${id}-categoria`} className="md-busca-rotulo">
            Categoria
          </label>
          <select
            id={`${id}-categoria`}
            className="form-select md-entrada"
            value={filtros.categoria}
            onChange={(e) => escolher('categoria', e.target.value)}
          >
            <option value="">Todas</option>
            {opcoes.categorias.map((c) => (
              <option key={c.codigo} value={c.codigo}>
                {c.descricao}
              </option>
            ))}
          </select>
        </div>
        <div className="md-busca-filtro">
          <label htmlFor={`${id}-status`} className="md-busca-rotulo">
            Situação
          </label>
          <select
            id={`${id}-status`}
            className="form-select md-entrada"
            value={filtros.status}
            onChange={(e) => escolher('status', e.target.value)}
          >
            <option value="">Todas</option>
            {opcoes.status.map((s) => (
              <option key={s.codigo} value={s.codigo}>
                {s.descricao}
              </option>
            ))}
          </select>
        </div>
      </div>

      {podeLimpar && (
        <button type="button" className="md-acao-texto align-self-start" onClick={limpar}>
          Limpar
        </button>
      )}
    </form>
  );
}
