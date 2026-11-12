import { useState } from 'react';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { fotosDaCategoria } from './opcoes.js';
import iconeCadeado from '../../assets/icones/cadeado.svg';

/**
 * 15 · Foto / Prévia. Não há upload (docs/api.md, pedido 5): a pessoa escolhe uma das
 * imagens de demonstração da categoria e só o caminho vai em `imagemUrl`.
 */
export default function EscolherFoto({ categoria, atual, aoUsar, aoVoltar }) {
  const fotos = fotosDaCategoria(categoria);
  const [escolhida, setEscolhida] = useState(
    fotos.find((foto) => foto.caminho === atual) ?? fotos[0],
  );

  return (
    <Tela titulo="Adicionar foto" gap={12}>
      <Cabecalho titulo="Adicionar foto" aoVoltar={aoVoltar} />
      <p className="md-sobretitulo">Imagem de demonstração</p>
      <h2 className="md-titulo">Uma foto ajuda a explicar.</h2>
      <p className="md-corpo">Confira a imagem antes de anexar.</p>

      {escolhida ? (
        <>
          <img
            src={escolhida.caminho}
            alt={`Foto ilustrativa: ${escolhida.descricao}`}
            className="md-foto-previa"
            style={{ height: 280 }}
          />
          {fotos.length > 1 && (
            <div className="d-flex gap-2 flex-wrap" role="group" aria-label="Imagens disponíveis">
              {fotos.map((foto) => (
                <button
                  key={foto.caminho}
                  type="button"
                  className={`md-miniatura ${foto === escolhida ? 'ativo' : ''}`}
                  aria-pressed={foto === escolhida}
                  aria-label={foto.descricao}
                  onClick={() => setEscolhida(foto)}
                >
                  <img src={foto.caminho} alt="" className="md-foto-previa" style={{ height: 60 }} />
                </button>
              ))}
            </div>
          )}
        </>
      ) : (
        <Mensagem tipo="info">
          Ainda não há imagem de demonstração para esta categoria. Você pode publicar a denúncia sem foto.
        </Mensagem>
      )}

      <div className="md-informacao">
        <img src={iconeCadeado} alt="" width="20" height="20" />
        <p className="md-legenda">Imagem ilustrativa. Nenhum arquivo pessoal é acessado neste protótipo.</p>
      </div>

      {escolhida && (
        <button type="button" className="md-botao md-botao-primario" onClick={() => aoUsar(escolhida.caminho)}>
          Usar esta foto
        </button>
      )}
      <button type="button" className="md-botao md-botao-secundario" onClick={aoVoltar}>
        Voltar
      </button>
    </Tela>
  );
}
