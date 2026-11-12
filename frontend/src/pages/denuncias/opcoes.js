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
 * Fotos do Wikimedia Commons e do Flickr (créditos no frontend/README.md), com miniaturas em img/demo/mini.
 */
export const FOTOS_DEMO = [
  { caminho: 'img/demo/asfalto-1.jpg', categoria: 'ASFALTO', descricao: 'Buracos remendados de forma precária no asfalto de uma rodovia, com um caminhão estacionado ao fundo' },
  { caminho: 'img/demo/asfalto-2.jpg', categoria: 'ASFALTO', descricao: 'Buraco fundo e cheio de água da chuva no asfalto de uma rua' },
  { caminho: 'img/demo/asfalto-3.jpg', categoria: 'ASFALTO', descricao: 'Trecho de estrada com grandes buracos e asfalto desgastado' },
  { caminho: 'img/demo/iluminacao-1.jpg', categoria: 'ILUMINACAO', descricao: 'Poste de iluminação derrubado e quebrado sobre o canteiro, ao lado de um banco' },
  { caminho: 'img/demo/iluminacao-2.jpg', categoria: 'ILUMINACAO', descricao: 'Rua residencial à noite, mal iluminada, com poucos postes acesos e trechos no escuro' },
  { caminho: 'img/demo/iluminacao-3.jpg', categoria: 'ILUMINACAO', descricao: 'Poste de luz caído no chão, à beira de um caminho de terra, perto de um lago' },
  { caminho: 'img/demo/calcada-acessibilidade-1.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada de mosaico com pedras soltas, buracos e tampas danificadas' },
  { caminho: 'img/demo/calcada-acessibilidade-2.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada rachada e desnivelada junto ao meio-fio, com um carro estacionado' },
  { caminho: 'img/demo/calcada-acessibilidade-3.jpg', categoria: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada quebrada com entulho e piso tátil interrompido junto a um muro' },
  { caminho: 'img/demo/seguranca-1.jpg', categoria: 'SEGURANCA', descricao: 'Sobrado abandonado e em ruínas, cercado por tapume na calçada' },
  { caminho: 'img/demo/seguranca-2.jpg', categoria: 'SEGURANCA', descricao: 'Rua vazia com portas de aço fechadas e paredes pichadas' },
  { caminho: 'img/demo/seguranca-3.jpg', categoria: 'SEGURANCA', descricao: 'Rua escura e vazia à noite, iluminada por poucos postes' },
  { caminho: 'img/demo/limpeza-urbana-1.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Sacos de lixo, caixas e entulho acumulados na calçada ao lado de carros estacionados' },
  { caminho: 'img/demo/limpeza-urbana-2.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Lixo, pneus velhos e entulho descartados junto a um muro na beira da rua' },
  { caminho: 'img/demo/limpeza-urbana-3.jpg', categoria: 'LIMPEZA_URBANA', descricao: 'Lixo espalhado em um terreno baldio com mato alto' },
  { caminho: 'img/demo/outros-1.jpg', categoria: 'OUTROS', descricao: 'Árvore caída arrancada pela raiz em uma praça' },
  { caminho: 'img/demo/outros-2.jpg', categoria: 'OUTROS', descricao: 'Rua alagada com água barrenta cobrindo as calçadas' },
  { caminho: 'img/demo/outros-3.jpg', categoria: 'OUTROS', descricao: 'Vazamento de água escorrendo pelo asfalto de uma rua' },
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
