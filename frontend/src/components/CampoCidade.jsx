import { useId } from 'react';
import { UFS } from '../utils/validacao.js';
import iconePin from '../assets/icones/pin-campo.svg';

/**
 * "Cidade e estado" (tela 03). A API precisa de cidade e UF separados (docs/api.md, pedido 3),
 * então o campo do Figma vira uma entrada de cidade com o pin e uma lista de estados ao lado.
 */
export default function CampoCidade({ cidade, uf, aoMudar, erros = {}, rotulo = 'Cidade e estado' }) {
  const id = useId();
  const erroCidade = erros.cidade;
  const erroUf = erros.uf;

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
            onChange={(e) => aoMudar({ cidade: e.target.value })}
            placeholder="Sua cidade"
            autoComplete="address-level2"
            maxLength={80}
            aria-invalid={erroCidade ? true : undefined}
            aria-describedby={erroCidade ? `${id}-erro-cidade` : undefined}
          />
          <span className="md-icone-campo" aria-hidden="true">
            <img src={iconePin} alt="" width="20" height="20" />
          </span>
        </div>
        <label htmlFor={`${id}-uf`} className="md-oculto-visualmente">
          Estado
        </label>
        <select
          id={`${id}-uf`}
          className={`form-select md-entrada md-campo-uf ${erroUf ? 'is-invalid' : ''}`}
          value={uf}
          onChange={(e) => aoMudar({ uf: e.target.value })}
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
      {erroCidade && (
        <p id={`${id}-erro-cidade`} className="md-erro-campo" role="alert">
          {erroCidade}
        </p>
      )}
      {erroUf && (
        <p id={`${id}-erro-uf`} className="md-erro-campo" role="alert">
          {erroUf}
        </p>
      )}
    </fieldset>
  );
}
