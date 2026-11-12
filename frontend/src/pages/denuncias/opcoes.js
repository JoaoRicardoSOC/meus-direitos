/**
 * Dados fixos da seção "03 · Participe": ícones das categorias (tela 14),
 * fotos de demonstração (tela 15) e o exemplo do botão "Preencher exemplo" (tela 12).
 */
import iconePin from '../../assets/icones/pin.svg';
import iconePessoa from '../../assets/icones/pessoa.svg';
import iconeEscudo from '../../assets/icones/shield.svg';
import iconeCasa from '../../assets/icones/casa.svg';
import iconeMais from '../../assets/icones/plus.svg';

/** Categorias da API (docs/api.md, 8.1). Usadas só se GET /categorias falhar. */
export const CATEGORIAS_PADRAO = [
  { codigo: 'ASFALTO', descricao: 'Asfalto' },
  { codigo: 'ILUMINACAO', descricao: 'Iluminação' },
  { codigo: 'CALCADA_ACESSIBILIDADE', descricao: 'Calçada e acessibilidade' },
  { codigo: 'SEGURANCA', descricao: 'Segurança no bairro' },
  { codigo: 'LIMPEZA_URBANA', descricao: 'Limpeza urbana' },
  { codigo: 'OUTROS', descricao: 'Outros' },
];

/** Ícone de cada categoria na tela 14 (o "Ícone/bulb" da Iluminação não pôde ser baixado do Figma). */
export const ICONE_CATEGORIA = {
  ASFALTO: iconePin,
  CALCADA_ACESSIBILIDADE: iconePessoa,
  SEGURANCA: iconeEscudo,
  LIMPEZA_URBANA: iconeCasa,
  OUTROS: iconeMais,
};

/**
 * Imagens de demonstração em public/img/demo (docs/api.md, 8.10).
 * Só existem as fotos que o Figma traz: hoje, apenas a de asfalto danificado.
 */
export const FOTOS_DEMO = [
  { caminho: 'img/demo/asfalto-1.jpg', categoria: 'ASFALTO', descricao: 'Asfalto danificado' },
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
