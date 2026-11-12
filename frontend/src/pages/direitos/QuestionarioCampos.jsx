import { forwardRef, useId } from 'react';
import iconeSeta from '../../assets/icones/seta-campo.svg';
import iconeCheck from '../../assets/icones/check-pequeno.svg';

const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

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
        aria-invalid={erro ? true : undefined}
      >
        <span id={`${id}-valor`} className={`md-questionario-escolha-valor ${valor ? '' : 'md-questionario-escolha-vazio'}`}>
          {valor ?? 'Toque para escolher'}
        </span>
        <img src={iconeSeta} alt="" width="20" height="20" />
      </button>
      {erro && (
        <p id={`${id}-erro`} className="md-erro-campo">
          {erro}
        </p>
      )}
    </div>
  );
});

/**
 * Data de nascimento em três campos (Dia, Mês por nome, Ano), mais fácil que o calendário nativo
 * no celular. `valor` = { dia, mes, ano } em texto; o ISO é montado por quem usa.
 */
export function CampoData({ legenda, valor, erro, aoMudar }) {
  const id = useId();
  const idErro = `${id}-erro`;
  const invalido = erro ? true : undefined;
  const descrito = erro ? idErro : undefined;
  const soDigitos = (texto, max) => texto.replace(/\D/g, '').slice(0, max);

  return (
    <fieldset className="md-campo md-questionario-data border-0 p-0 m-0">
      <legend className="md-campo-rotulo">{legenda}</legend>
      <div className="md-questionario-data-partes">
        <div className="md-questionario-data-dia">
          <label htmlFor={`${id}-dia`} className="md-legenda">
            Dia
          </label>
          <input
            id={`${id}-dia`}
            className={`form-control md-entrada ${erro ? 'is-invalid' : ''}`}
            inputMode="numeric"
            autoComplete="bday-day"
            maxLength={2}
            placeholder="DD"
            value={valor.dia}
            onChange={(e) => aoMudar({ ...valor, dia: soDigitos(e.target.value, 2) })}
            aria-invalid={invalido}
            aria-describedby={descrito}
          />
        </div>
        <div className="md-questionario-data-mes">
          <label htmlFor={`${id}-mes`} className="md-legenda">
            Mês
          </label>
          <select
            id={`${id}-mes`}
            className={`form-select md-entrada ${erro ? 'is-invalid' : ''}`}
            autoComplete="bday-month"
            value={valor.mes}
            onChange={(e) => aoMudar({ ...valor, mes: e.target.value })}
            aria-invalid={invalido}
            aria-describedby={descrito}
          >
            <option value="">Mês</option>
            {MESES.map((nome, i) => (
              <option key={nome} value={String(i + 1).padStart(2, '0')}>
                {nome}
              </option>
            ))}
          </select>
        </div>
        <div className="md-questionario-data-ano">
          <label htmlFor={`${id}-ano`} className="md-legenda">
            Ano
          </label>
          <input
            id={`${id}-ano`}
            className={`form-control md-entrada ${erro ? 'is-invalid' : ''}`}
            inputMode="numeric"
            autoComplete="bday-year"
            maxLength={4}
            placeholder="AAAA"
            value={valor.ano}
            onChange={(e) => aoMudar({ ...valor, ano: soDigitos(e.target.value, 4) })}
            aria-invalid={invalido}
            aria-describedby={descrito}
          />
        </div>
      </div>
      {erro && (
        <p id={idErro} className="md-erro-campo">
          {erro}
        </p>
      )}
    </fieldset>
  );
}

/** Marca da opção escolhida: o check à direita, além da borda e do fundo (não depende só da cor). */
function MarcaSelecionada() {
  return <img className="md-questionario-opcao-check" src={iconeCheck} alt="" width="20" height="20" />;
}

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
              <span className="md-questionario-opcao-texto">{opcao.descricao}</span>
              {ativa && <MarcaSelecionada />}
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
      <legend className="md-campo-rotulo">{rotulo}</legend>
      <div className="md-questionario-opcoes">
        {opcoes.map((opcao) => {
          const ativa = valor === opcao.valor;
          return (
            <label key={opcao.texto} className={`md-questionario-opcao ${ativa ? 'selecionada' : ''}`}>
              <input
                type="radio"
                name={id}
                className="md-oculto-visualmente"
                checked={ativa}
                onChange={() => aoMudar(opcao.valor)}
                aria-invalid={erro ? true : undefined}
              />
              <span className="md-questionario-opcao-texto">{opcao.texto}</span>
              {ativa && <MarcaSelecionada />}
            </label>
          );
        })}
      </div>
      {erro && (
        <p id={`${id}-erro`} className="md-erro-campo">
          {erro}
        </p>
      )}
    </fieldset>
  );
}
