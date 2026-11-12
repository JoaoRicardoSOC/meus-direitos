import { useState } from 'react';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { fotosDaCategoria } from './opcoes.js';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import iconeCheck from '../../assets/icones/check-pequeno.svg';

/** Miniatura de 400 px (public/img/demo/mini/<mesmo nome>). */
function caminhoDaMiniatura(caminho) {
  return caminho.replace('img/demo/', 'img/demo/mini/');
}

/** Sem a miniatura, usa a foto grande (uma vez só, para não repetir o erro). */
function usarFotoGrande(evento, caminho) {
  const imagem = evento.currentTarget;
  if (imagem.dataset.reserva) return;
  imagem.dataset.reserva = '1';
  imagem.src = caminho;
}

/**
 * 15 · Foto / Prévia (?passo=foto). Não há upload (docs/api.md, pedido 5): a pessoa escolhe uma das
 * imagens de demonstração da categoria e só o caminho vai em `imagemUrl`.
 */
export default function EscolherFoto({ categoria, atual, aoUsar, aoVoltar }) {
  const fotos = fotosDaCategoria(categoria);
  const [escolhida, setEscolhida] = useState(
    fotos.find((foto) => foto.caminho === atual) ?? fotos[0],
  );

  return (
    <Tela titulo="Adicionar foto">
      <div key="foto" className="md-etapa">
        <Cabecalho titulo="Adicionar foto" aoVoltar={aoVoltar} />
        <div className="d-flex flex-column gap-2">
          <h2 className="md-titulo">
            Uma foto ajuda
            <br />a explicar.
          </h2>
          <p className="md-corpo">Confira a imagem antes de anexar.</p>
        </div>

        {escolhida ? (
          <div className="d-flex flex-column gap-3 md-grupo">
            <img
              src={escolhida.caminho}
              alt={`Foto ilustrativa: ${escolhida.descricao}`}
              className="md-foto-previa md-foto-grande"
            />
            {fotos.length > 1 && (
              <div className="md-miniaturas" role="group" aria-label="Imagens disponíveis">
                {fotos.map((foto) => {
                  const ativo = foto === escolhida;
                  return (
                    <button
                      key={foto.caminho}
                      type="button"
                      className={`md-miniatura ${ativo ? 'ativo' : ''}`}
                      aria-pressed={ativo}
                      aria-label={foto.descricao}
                      onClick={() => setEscolhida(foto)}
                    >
                      <img
                        src={caminhoDaMiniatura(foto.caminho)}
                        alt=""
                        className="md-miniatura-foto"
                        loading="lazy"
                        decoding="async"
                        onError={(evento) => usarFotoGrande(evento, foto.caminho)}
                      />
                      {ativo && (
                        <span className="md-miniatura-check" aria-hidden="true">
                          <img src={iconeCheck} alt="" width="16" height="16" />
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          <Mensagem tipo="info">
            Ainda não há imagem de demonstração para esta categoria. Você pode publicar a denúncia sem foto.
          </Mensagem>
        )}

        <div className="md-informacao">
          <img src={iconeCadeado} alt="" width="20" height="20" />
          <p className="md-legenda">Imagem ilustrativa. Nenhum arquivo pessoal é acessado neste protótipo.</p>
        </div>

        <div className="d-flex flex-column gap-3 md-grupo">
          {escolhida && (
            <button type="button" className="md-botao md-botao-primario" onClick={() => aoUsar(escolhida.caminho)}>
              Usar esta foto
            </button>
          )}
          <button type="button" className="md-botao md-botao-secundario" onClick={aoVoltar}>
            Voltar
          </button>
        </div>
      </div>
    </Tela>
  );
}
