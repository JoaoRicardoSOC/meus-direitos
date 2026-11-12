/**
 * Dados fixos da seção "03 · Participe": ícones das categorias (tela 14),
 * fotos de demonstração (tela 15) e o exemplo do botão "Preencher exemplo" (tela 12).
 */
import iconePin from '../../assets/icones/pin.svg';
import iconeLampada from '../../assets/icones/lampada.svg';
import iconePessoa from '../../assets/icones/pessoa.svg';
import iconeEscudo from '../../assets/icones/shield.svg';
import iconeCasa from '../../assets/icones/casa.svg';
import iconeMais from '../../assets/icones/mais.svg';

/** Categorias da API (docs/api.md, 8.1). Usadas só se GET /categorias falhar. */
export const CATEGORIAS_PADRAO = [
  { codigo: 'ASFALTO', descricao: 'Asfalto' },
  { codigo: 'ILUMINACAO', descricao: 'Iluminação' },
  { codigo: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada e acessibilidade' },
  { codigo: 'SEGURANCA', descricao: 'Segurança no bairro' },
  { codigo: 'LIMPEZA_URBANA', descricao: 'Limpeza urbana' },
  { codigo: 'OUTROS', descricao: 'Outros' },
];

/** Ícone de cada categoria na tela 14. */
export const ICONE_CATEGORIA = {
  ASFALTO: iconePin,
  ILUMINACAO: iconeLampada,
  CALCADA_ACESSIBILIDADE: iconePessoa,
  SEGURANCA: iconeEscudo,
  LIMPEZA_URBANA: iconeCasa,
  OUTROS: iconeMais,
};

/**
 * Imagens de demonstração em public/img/demo (docs/api.md, 8.10), três por categoria.
 * asfalto-1 vem do Figma; as demais, do Wikimedia Commons (créditos no frontend/README.md).
 */
export const FOTOS_DEMO = [
  { caminho: 'img/demo/asfalto-1.jpg', categoria: 'ASFALTO', descricao: 'Asfalto danificado' },
  { caminho: 'img/demo/asfalto-2.jpg', categoria: 'ASFALTO', descricao: 'Buraco com água na rua' },
  { caminho: 'img/demo/asfalto-3.jpg', categoria: 'ASFALTO', descricao: 'Rua com vários buracos' },
  { caminho: 'img/demo/iluminacao-1.jpg', categoria: 'ILUMINACAO', descricao: 'Postes de iluminação pública' },
  { caminho: 'img/demo/iluminacao-2.jpg', categoria: 'ILUMINACAO', descricao: 'Rua à noite com postes acesos' },
  { caminho: 'img/demo/iluminacao-3.jpg', categoria: 'ILUMINACAO', descricao: 'Luminária de rua acesa' },
  { caminho: 'img/demo/calcada-acessibilidade-1.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada levantada por raiz de árvore' },
  { caminho: 'img/demo/calcada-acessibilidade-2.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada rachada' },
  { caminho: 'img/demo/calcada-acessibilidade-3.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Rampa de acessibilidade danificada' },
  { caminho: 'img/demo/seguranca-1.jpg', categoria: 'SEGURANCA', descricao: 'Rua pouco iluminada à noite' },
  { caminho: 'img/demo/seguranca-2.jpg', categoria: 'SEGURANCA', descricao: 'Viela à noite' },
  { caminho: 'img/demo/seguranca-3.jpg', categoria: 'SEGURANCA', descricao: 'Beco estreito à noite' },
  { caminho: 'img/demo/limpeza-urbana-1.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Sacos de lixo espalhados na rua' },
  { caminho: 'img/demo/limpeza-urbana-2.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Lixo acumulado na calçada' },
  { caminho: 'img/demo/limpeza-urbana-3.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Lixo descartado em terreno' },
  { caminho: 'img/demo/outros-1.jpg', categoria: 'OUTROS', descricao: 'Árvore caída sobre a grade' },
  { caminho: 'img/demo/outros-2.jpg', categoria: 'OUTROS', descricao: 'Vazamento de água na rua' },
  { caminho: 'img/demo/outros-3.jpg', categoria: 'OUTROS', descricao: 'Árvore caída bloqueando a passagem' },
];

/** Fotos da categoria escolhida; sem categoria, todas. */
export function fotosDaCategoria(codigo) {
  return codigo ? FOTOS_DEMO.filter((foto) => foto.categoria === codigo) : FOTOS_DEMO;
}

export function descricaoDaFoto(caminho) {
  return FOTOS_DEMO.find((foto) => foto.caminho === caminho)?.descricao ?? 'Foto da denúncia';
}

/** Exemplo realista para o botão "Preencher exemplo" (tela 13 preenchida). */
export const EXEMPLO = {
  titulo: 'Buraco próximo à faixa de pedestres',
  descricao: 'O asfalto cedeu perto da faixa e dificulta a travessia de moradores, principalmente de idosos e de quem usa cadeira de rodas.',
  categoria: 'ASFALTO',
  endereco: 'Rua das Flores, 120',
  bairro: 'Centro',
  imagemUrl: 'img/demo/asfalto-1.jpg',
};
