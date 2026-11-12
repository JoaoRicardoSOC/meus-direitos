import { useState } from 'react';
import fotoAsfalto from '../assets/imagens/asfalto-danificado.jpg';

/**
 * Foto padrão por categoria, usada quando a denúncia não tem imagem ou a imagem não carrega.
 * A de asfalto vai no pacote do app; as outras são as fotos de demonstração de public/img/demo.
 */
const FOTO_PADRAO = {
  ASFALTO: fotoAsfalto,
  ILUMINACAO: 'img/demo/iluminacao-1.jpg',
  CALCADA_ACESSIBILIDADE: 'img/demo/calcada-acessibilidade-1.jpg',
  SEGURANCA: 'img/demo/seguranca-1.jpg',
  LIMPEZA_URBANA: 'img/demo/limpeza-urbana-1.jpg',
  OUTROS: 'img/demo/outros-1.jpg',
};

/**
 * Foto da denúncia. `imagemUrl` é um caminho do front (img/demo/asfalto-1.jpg, em public/)
 * ou um endereço https://. Sem imagem, mostra a foto padrão da categoria ou um bloco com o nome dela.
 */
export default function ImagemDenuncia({ imagemUrl, categoria, altura = 120 }) {
  const [falhou, setFalhou] = useState(false);
  const padrao = FOTO_PADRAO[categoria?.codigo];
  const fonte = !falhou && imagemUrl ? imagemUrl : padrao;
  const estilo = { height: altura, borderRadius: 12, width: '100%' };

  if (!fonte) {
    return (
      <div
        className="d-flex align-items-center justify-content-center md-sobretitulo"
        style={{ ...estilo, background: 'var(--md-suave)' }}
        aria-hidden="true"
      >
        {categoria?.descricao}
      </div>
    );
  }

  return (
    <img
      src={fonte}
      alt={categoria ? `Foto ilustrativa: ${categoria.descricao}` : ''}
      style={{ ...estilo, objectFit: 'cover', display: 'block' }}
      onError={() => setFalhou(true)}
    />
  );
}
