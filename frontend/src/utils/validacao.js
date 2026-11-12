/**
 * Validação em JavaScript antes de chamar a API (rubrica Web da FIAP).
 * As regras são as mesmas do back-end (docs/api.md), então a API raramente precisa recusar.
 * Cada função devolve a mensagem de erro, ou '' quando o valor é válido.
 */

export const UFS = [
  ['AC', 'Acre'], ['AL', 'Alagoas'], ['AP', 'Amapá'], ['AM', 'Amazonas'], ['BA', 'Bahia'],
  ['CE', 'Ceará'], ['DF', 'Distrito Federal'], ['ES', 'Espírito Santo'], ['GO', 'Goiás'],
  ['MA', 'Maranhão'], ['MT', 'Mato Grosso'], ['MS', 'Mato Grosso do Sul'], ['MG', 'Minas Gerais'],
  ['PA', 'Pará'], ['PB', 'Paraíba'], ['PR', 'Paraná'], ['PE', 'Pernambuco'], ['PI', 'Piauí'],
  ['RJ', 'Rio de Janeiro'], ['RN', 'Rio Grande do Norte'], ['RS', 'Rio Grande do Sul'],
  ['RO', 'Rondônia'], ['RR', 'Roraima'], ['SC', 'Santa Catarina'], ['SP', 'São Paulo'],
  ['SE', 'Sergipe'], ['TO', 'Tocantins'],
];

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function obrigatorio(valor, mensagem = 'Campo obrigatório.') {
  return String(valor ?? '').trim() ? '' : mensagem;
}

export function tamanho(valor, min, max) {
  const texto = String(valor ?? '').trim();
  if (!texto) return min > 0 ? 'Campo obrigatório.' : '';
  if (texto.length < min) return `Deve ter pelo menos ${min} caracteres.`;
  if (texto.length > max) return `Deve ter no máximo ${max} caracteres.`;
  return '';
}

export const validar = {
  nome: (v) => tamanho(v, 3, 100),
  email: (v) => {
    const texto = String(v ?? '').trim();
    if (!texto) return 'Campo obrigatório.';
    return texto.length <= 120 && EMAIL.test(texto) ? '' : 'E-mail em formato inválido.';
  },
  cidade: (v) => tamanho(v, 2, 80),
  uf: (v) => (UFS.some(([sigla]) => sigla === v) ? '' : 'Escolha o estado.'),
  senha: (v) => {
    const texto = String(v ?? '');
    if (!texto) return 'Campo obrigatório.';
    if (texto.length < 8 || texto.length > 64) return 'A senha deve ter de 8 a 64 caracteres.';
    if (!/\p{L}/u.test(texto) || !/\d/.test(texto)) return 'Use pelo menos uma letra e um número.';
    return '';
  },
};

/** Aplica as regras e devolve só os campos com erro: { nome: '...', email: '...' } */
export function validarCampos(valores, regras) {
  const erros = {};
  Object.entries(regras).forEach(([campo, regra]) => {
    const mensagem = regra(valores[campo], valores);
    if (mensagem) erros[campo] = mensagem;
  });
  return erros;
}
