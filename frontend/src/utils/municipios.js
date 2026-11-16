import lista from '../data/municipios.json';
import { chaveTexto } from './formatos.js';

/**
 * Municípios do IBGE por UF (src/data/municipios.json, o mesmo arquivo da API em
 * backend/src/main/resources/municipios.json). Fica dentro do app (cerca de 30 KB comprimido), para o
 * cadastro funcionar offline e com o index.html aberto direto do disco.
 */

const porChave = {}; // { SP: Map('sao paulo' → 'São Paulo'), ... }, montado na primeira consulta de cada UF

function mapaDoEstado(uf) {
  if (!lista[uf]) return null;
  if (!porChave[uf]) porChave[uf] = new Map(lista[uf].map((nome) => [chaveTexto(nome), nome]));
  return porChave[uf];
}

/** Todas as cidades do estado, em ordem alfabética (lista vazia para UF desconhecida). */
export function cidadesDoEstado(uf) {
  return lista[uf] ?? [];
}

/** "sao paulo", "SP" → "São Paulo"; '' se a cidade não existir no estado. */
export function nomeOficial(cidade, uf) {
  return mapaDoEstado(uf)?.get(chaveTexto(cidade)) ?? '';
}

/**
 * Sugestões para o texto digitado, ignorando acentos e maiúsculas: primeiro as que começam com o texto,
 * depois as que o contêm no meio ("campos" → "Campos do Jordão"… e depois "São José dos Campos").
 * Sem texto, todas as cidades do estado em ordem alfabética.
 */
export function sugerirCidades(texto, uf) {
  const cidades = cidadesDoEstado(uf);
  const busca = chaveTexto(texto);
  if (!busca) return cidades;
  const comecam = [];
  const contem = [];
  cidades.forEach((nome) => {
    const chave = chaveTexto(nome);
    if (chave.startsWith(busca)) comecam.push(nome);
    else if (chave.includes(busca)) contem.push(nome);
  });
  return comecam.concat(contem);
}
