import { useState } from 'react';

/** Foto padrão por categoria (fotos de demonstração de public/img/demo), quando a denúncia não tem foto. */
const FOTO_PADRAO = {
  ASFALTO: 'img/demo/asfalto-1.jpg',
  ILUMINACAO: 'img/demo/iluminacao-1.jpg',
  CALCADA_ACESSIBILIDADE: 'img/demo/calcada-acessibilidade-1.jpg',
  SEGURANCA: 'img/demo/seguranca-1.jpg',
  LIMPEZA_URBANA: 'img/demo/limpeza-urbana-1.jpg',
  OUTROS: 'img/demo/outros-1.jpg',
};

/**
 * Foto da denúncia. `imagemUrl` é um caminho do front (img/demo/asfalto-1.jpg, em public/)
 * ou um endereço https://. Sem imagem, mostra a foto padrão da categoria ou um bloco com o nome dela.
 * `proporcao`: "16 / 9" no feed, "4 / 3" no detalhe. A altura fica reservada antes de a foto chegar.
 */
export default function ImagemDenuncia({ imagemUrl, categoria, proporcao = '16 / 9' }) {
  const [falhou, setFalhou] = useState(false);
  const [carregada, setCarregada] = useState(false);
  const padrao = FOTO_PADRAO[categoria?.codigo];
  const fonte = !falhou && imagemUrl ? imagemUrl : padrao;
  const estilo = { aspectRatio: proporcao, borderRadius: 12, width: '100%' };

  if (!fonte) {
    return (
      <div
        className="d-flex align-items-center justify-content-center md-legenda"
        style={{ ...estilo, background: 'var(--md-suave)' }}
        aria-hidden="true"
      >
        {categoria?.descricao}
      </div>
    );
  }

  return (
    <div style={{ ...estilo, background: 'var(--md-esqueleto)', overflow: 'hidden' }}>
      <img
        src={fonte}
        alt={categoria ? `Foto ilustrativa: ${categoria.descricao}` : ''}
        loading="lazy"
        decoding="async"
        className="md-foto"
        data-carregada={carregada ? '' : undefined}
        style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        onLoad={() => setCarregada(true)}
        onError={() => {
          if (!falhou) setFalhou(true);
          else setCarregada(true); // a padrão também falhou: mostra o que houver
        }}
      />
    </div>
  );
}
