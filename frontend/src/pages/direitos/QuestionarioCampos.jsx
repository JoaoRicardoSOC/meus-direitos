import { forwardRef, useId } from 'react';
import iconeSeta from '../../assets/icones/seta-campo.svg';

/**
 * Campo "Como você se declara?" / "Possui alguma deficiência?" (telas 23 e 25):
 * parece uma entrada com a seta para baixo e abre a tela de opções ao ser tocado.
 */
export const CampoEscolha = forwardRef(function CampoEscolha({ rotulo, valor, erro, aoAbrir }, ref) {
  const id = useId();
  return (
    <div className="md-campo">
      <p id={`${id}-rotulo`} className="md-campo-rotulo">
        {rotulo}
      </p>
      <button
        ref={ref}
        type="button"
        id={id}
        className={`md-questionario-escolha ${erro ? 'is-invalid' : ''}`}
        onClick={aoAbrir}
        aria-labelledby={`${id}-rotulo ${id}-valor`}
        aria-describedby={erro ? `${id}-erro` : undefined}
      >
        <span id={`${id}-valor`} className={`md-questionario-escolha-valor ${valor ? '' : 'md-questionario-escolha-vazio'}`}>
          {valor ?? 'Selecionar'}
        </span>
        <img src={iconeSeta} alt="" width="20" height="20" />
      </button>
      {erro && (
        <p id={`${id}-erro`} className="md-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </div>
  );
});

/** Lista de opções das telas 24, 26 e 27: tocar em uma opção escolhe e volta para a etapa. */
export function ListaOpcoes({ opcoes, selecionada, rotuladaPor, aoEscolher }) {
  if (opcoes.length === 0) {
    return <p className="md-corpo">Nenhuma opção disponível no momento.</p>;
  }
  return (
    <ul className="md-questionario-opcoes" aria-labelledby={rotuladaPor}>
      {opcoes.map((opcao) => {
        const ativa = opcao.codigo === selecionada;
        return (
          <li key={opcao.codigo}>
            <button
              type="button"
              className={`md-questionario-opcao ${ativa ? 'selecionada' : ''}`}
              aria-pressed={ativa}
              onClick={() => aoEscolher(opcao.codigo)}
            >
              {opcao.descricao}
            </button>
          </li>
        );
      })}
    </ul>
  );
}

/** Pergunta de "Sim" ou "Não" no mesmo estilo das opções (true / false; sem resposta = null). */
export function PerguntaSimNao({ rotulo, valor, erro, aoMudar }) {
  const id = useId();
  const opcoes = [
    { valor: true, texto: 'Sim' },
    { valor: false, texto: 'Não' },
  ];
  return (
    <fieldset className="md-campo border-0 p-0 m-0" aria-describedby={erro ? `${id}-erro` : undefined}>
      <legend className="md-campo-rotulo mb-2">{rotulo}</legend>
      <div className="md-questionario-opcoes">
        {opcoes.map((opcao) => (
          <label key={opcao.texto} className={`md-questionario-opcao ${valor === opcao.valor ? 'selecionada' : ''}`}>
            <input
              type="radio"
              name={id}
              className="md-oculto-visualmente"
              checked={valor === opcao.valor}
              onChange={() => aoMudar(opcao.valor)}
            />
            {opcao.texto}
          </label>
        ))}
      </div>
      {erro && (
        <p id={`${id}-erro`} className="md-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </fieldset>
  );
}
