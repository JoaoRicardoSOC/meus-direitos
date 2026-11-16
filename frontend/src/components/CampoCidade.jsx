import { useEffect, useId, useMemo, useState } from 'react';
import { UFS } from '../utils/validacao.js';
import { nomeOficial, sugerirCidades } from '../utils/municipios.js';
import { numero } from '../utils/formatos.js';
import iconePin from '../assets/icones/pin-campo.svg';

/**
 * "Cidade e estado" (tela 03). A API precisa de cidade e UF separados (docs/api.md, pedido 3),
 * então o campo do Figma vira uma entrada de cidade com o pin e uma lista de estados ao lado.
 * <p>
 * A cidade só libera depois do estado e sugere os municípios do IBGE daquele estado (utils/municipios.js):
 * vazia, mostra todos em ordem alfabética; a cada letra, a lista se refaz ignorando acentos. Segue o padrão
 * "combobox com lista" da WAI-ARIA: setas percorrem as sugestões, Enter escolhe, Esc fecha, e o total de
 * sugestões é anunciado ao leitor de tela. Ao sair do campo, um nome digitado igual a um da lista vira a
 * grafia oficial ("sao paulo" → "São Paulo"); um nome fora da lista recebe a mensagem da validação.
 */
export default function CampoCidade({ cidade, uf, aoMudar, erros = {}, rotulo = 'Cidade e estado' }) {
  const id = useId();
  const idLista = `${id}-sugestoes`;
  const erroCidade = erros.cidade;
  const erroUf = erros.uf;
  const temEstado = Boolean(uf);

  const [aberta, setAberta] = useState(false);
  const [ativa, setAtiva] = useState(-1); // índice da sugestão destacada pelo teclado
  const sugestoes = useMemo(() => (temEstado ? sugerirCidades(cidade, uf) : []), [cidade, uf, temEstado]);
  const mostrarLista = aberta && temEstado;

  // A sugestão destacada pelo teclado fica visível dentro da lista com rolagem
  useEffect(() => {
    if (mostrarLista && ativa >= 0) document.getElementById(`${id}-op-${ativa}`)?.scrollIntoView({ block: 'nearest' });
  }, [ativa, mostrarLista, id]);

  function escolher(nome) {
    aoMudar({ cidade: nome });
    setAberta(false);
    setAtiva(-1);
  }

  function aoDigitar(texto) {
    aoMudar({ cidade: texto });
    setAberta(true);
    setAtiva(texto.trim() ? 0 : -1); // com texto, Enter escolhe a primeira sugestão
  }

  function aoTeclar(e) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      if (!mostrarLista) {
        setAberta(true);
        setAtiva(0);
        return;
      }
      const passo = e.key === 'ArrowDown' ? 1 : -1;
      setAtiva((atual) => Math.min(Math.max(atual + passo, 0), sugestoes.length - 1));
    } else if (e.key === 'Enter' && mostrarLista && ativa >= 0 && sugestoes[ativa]) {
      e.preventDefault(); // escolhe a cidade sem enviar o formulário
      escolher(sugestoes[ativa]);
    } else if (e.key === 'Escape' && mostrarLista) {
      e.preventDefault();
      setAberta(false);
      setAtiva(-1);
    }
  }

  function aoSair() {
    setAberta(false);
    setAtiva(-1);
    const oficial = nomeOficial(cidade, uf);
    if (oficial && oficial !== cidade) aoMudar({ cidade: oficial });
  }

  function aoTrocarEstado(novaUf) {
    // A cidade escolhida não existe no novo estado: limpa, para a pessoa escolher de novo
    const manterCidade = !cidade || Boolean(nomeOficial(cidade, novaUf));
    aoMudar(manterCidade ? { uf: novaUf } : { uf: novaUf, cidade: '' });
  }

  return (
    <fieldset className="md-campo border-0 p-0 m-0">
      <legend className="md-campo-rotulo mb-2">{rotulo}</legend>
      <div className="d-flex gap-2">
        <div className="md-entrada-com-icone flex-grow-1">
          <label htmlFor={`${id}-cidade`} className="md-oculto-visualmente">
            Cidade
          </label>
          <input
            id={`${id}-cidade`}
            className={`form-control md-entrada ${erroCidade ? 'is-invalid' : ''}`}
            value={cidade}
            onChange={(e) => aoDigitar(e.target.value)}
            onFocus={() => setAberta(true)}
            onClick={() => setAberta(true)}
            onKeyDown={aoTeclar}
            onBlur={aoSair}
            disabled={!temEstado}
            placeholder={temEstado ? 'Sua cidade' : 'Escolha o estado'}
            autoComplete="off"
            maxLength={80}
            role="combobox"
            aria-autocomplete="list"
            aria-expanded={mostrarLista}
            aria-controls={idLista}
            aria-activedescendant={mostrarLista && ativa >= 0 ? `${id}-op-${ativa}` : undefined}
            aria-invalid={erroCidade ? true : undefined}
            aria-describedby={erroCidade ? `${id}-erro-cidade` : undefined}
          />
          <span className="md-icone-campo" aria-hidden="true">
            <img src={iconePin} alt="" width="20" height="20" />
          </span>
          <ul id={idLista} role="listbox" aria-label={`Cidades de ${uf || 'estado não escolhido'}`}
            className="md-sugestoes" hidden={!mostrarLista || sugestoes.length === 0}>
            {mostrarLista &&
              sugestoes.map((nome, i) => (
                <li
                  key={nome}
                  id={`${id}-op-${i}`}
                  role="option"
                  aria-selected={i === ativa}
                  className={`md-sugestao ${i === ativa ? 'ativa' : ''}`}
                  // mousedown em vez de click: escolhe antes de o campo perder o foco (e a lista fechar)
                  onMouseDown={(e) => {
                    e.preventDefault();
                    escolher(nome);
                  }}
                >
                  {nome}
                </li>
              ))}
          </ul>
          {mostrarLista && sugestoes.length === 0 && (
            <p className="md-sugestoes md-sugestoes-vazio">Nenhuma cidade com esse nome em {uf}.</p>
          )}
        </div>
        <label htmlFor={`${id}-uf`} className="md-oculto-visualmente">
          Estado
        </label>
        <select
          id={`${id}-uf`}
          className={`form-select md-entrada md-campo-uf ${erroUf ? 'is-invalid' : ''}`}
          value={uf}
          onChange={(e) => aoTrocarEstado(e.target.value)}
          autoComplete="address-level1"
          aria-invalid={erroUf ? true : undefined}
          aria-describedby={erroUf ? `${id}-erro-uf` : undefined}
        >
          <option value="">UF</option>
          {UFS.map(([sigla, nome]) => (
            <option key={sigla} value={sigla} title={nome}>
              {sigla}
            </option>
          ))}
        </select>
      </div>
      <p className="md-oculto-visualmente" role="status" aria-live="polite">
        {mostrarLista ? `${numero(sugestoes.length)} ${sugestoes.length === 1 ? 'cidade' : 'cidades'} na lista` : ''}
      </p>
      {erroCidade && (
        <p id={`${id}-erro-cidade`} className="md-erro-campo">
          {erroCidade}
        </p>
      )}
      {erroUf && (
        <p id={`${id}-erro-uf`} className="md-erro-campo">
          {erroUf}
        </p>
      )}
    </fieldset>
  );
}
