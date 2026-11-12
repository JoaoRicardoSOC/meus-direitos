/** Formatação de textos exibidos nas telas (docs/api.md, seção 10). */

const tempoRelativo = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });

const UNIDADES = [
  ['year', 60 * 60 * 24 * 365],
  ['month', 60 * 60 * 24 * 30],
  ['week', 60 * 60 * 24 * 7],
  ['day', 60 * 60 * 24],
  ['hour', 60 * 60],
  ['minute', 60],
];

/** "2026-11-11T17:13:00" → "há 2 horas". A API devolve horário local, sem fuso. */
export function haQuantoTempo(dataIso) {
  if (!dataIso) return '';
  const segundos = Math.round((new Date(dataIso).getTime() - Date.now()) / 1000);
  for (const [unidade, tamanho] of UNIDADES) {
    if (Math.abs(segundos) >= tamanho) {
      return tempoRelativo.format(Math.round(segundos / tamanho), unidade);
    }
  }
  return 'agora';
}

/** "Mariana Oliveira" → "MO"; "Ana" → "A". */
export function iniciais(nome) {
  const partes = (nome ?? '').trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return '?';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

/** "São Paulo", "SP" → "São Paulo, SP" */
export function cidadeUf(cidade, uf) {
  return [cidade, uf].filter(Boolean).join(', ');
}

/** "2026-11-11" → "11/11/2026" */
export function dataCurta(dataIso) {
  if (!dataIso) return '';
  const [ano, mes, dia] = dataIso.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

const numeros = new Intl.NumberFormat('pt-BR');

/** 1234 → "1.234" */
export function numero(valor) {
  return numeros.format(valor ?? 0);
}

/** plural(1, 'comentário', 'comentários') → "1 comentário"; plural(3, …) → "3 comentários" */
export function plural(quantidade, singular, pluralTexto) {
  return `${numero(quantidade)} ${quantidade === 1 ? singular : pluralTexto}`;
}

/** Remove acentos e caixa para comparar nomes de cidade ("São Paulo" = "sao paulo"). */
export function chaveTexto(texto) {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .trim()
    .replace(/\s+/g, ' ')
    .toLowerCase();
}
