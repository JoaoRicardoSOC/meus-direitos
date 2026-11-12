import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import { ICONE_CATEGORIA } from './opcoes.js';

/**
 * 14 · Categorias: lista de opções; escolher uma volta para o formulário (telas 12/13).
 */
export default function EscolherCategoria({ categorias, selecionada, aoEscolher, aoVoltar }) {
  return (
    <Tela titulo="Tipo de problema" gap={12}>
      <Cabecalho titulo="Tipo de problema" aoVoltar={aoVoltar} />
      <h2 className="md-titulo">Qual é a categoria?</h2>
      <p className="md-corpo">Escolha a opção que melhor descreve o relato.</p>

      <ul className="md-lista-limpa d-flex flex-column gap-2">
        {categorias.map((categoria) => {
          const ativo = categoria.codigo === selecionada;
          const icone = ICONE_CATEGORIA[categoria.codigo];
          return (
            <li key={categoria.codigo}>
              <button
                type="button"
                className={`md-opcao md-opcao-categoria ${ativo ? 'ativo' : ''}`}
                aria-pressed={ativo}
                onClick={() => aoEscolher(categoria.codigo)}
              >
                <span className="md-opcao-icone" aria-hidden="true">
                  {icone && <img src={icone} alt="" width="24" height="24" />}
                </span>
                {categoria.descricao}
              </button>
            </li>
          );
        })}
      </ul>
    </Tela>
  );
}
