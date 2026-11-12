import iconeCasa from '../../assets/icones/casa.svg';
import iconePessoa from '../../assets/icones/pessoa.svg';
import iconeLivro from '../../assets/icones/livro.svg';
import iconeEscudo from '../../assets/icones/shield.svg';

/**
 * Ícone de cada tema (campo `icone` da API, docs/api.md §8.4).
 * TRANSPORTE, PRIORIDADE e MUNICIPIO usam os ícones dos cards da tela 28 (home, user, book).
 * Os demais não têm desenho próprio no Figma: EDUCACAO usa o livro e os outros, o escudo de "Meus Direitos".
 */
const ICONES = {
  TRANSPORTE: { src: iconeCasa, tamanho: 24 },
  PRIORIDADE: { src: iconePessoa, tamanho: 24 },
  MUNICIPIO: { src: iconeLivro, tamanho: 24 },
  EDUCACAO: { src: iconeLivro, tamanho: 24 },
};

const GENERICO = { src: iconeEscudo, tamanho: 22 };

export function iconeDoTema(codigo) {
  return ICONES[codigo] ?? GENERICO;
}
