/**
 * Avisos de atividade em "Minhas denúncias", sem back-end novo.
 * Guarda no localStorage, por usuário, um retrato de cada denúncia da última vez que a pessoa viu
 * a lista ({ upvotes, downvotes, comentarios, confirmacoes, status }). Na visita seguinte, o que
 * subiu ou mudou vira "Novidades: +2 apoios, +1 comentário, status mudou para Resolvida".
 * Sem localStorage (aba privada, cota cheia), não há avisos: o resto funciona igual.
 */
import { plural } from './formatos.js';

function chave(usuarioId) {
  return `md-novidades:${usuarioId}`;
}

/** Os números que importam de uma DenunciaResponse. */
function retratoDe(denuncia) {
  return {
    upvotes: denuncia.placar?.upvotes ?? 0,
    downvotes: denuncia.placar?.downvotes ?? 0,
    comentarios: denuncia.comentarios ?? 0,
    confirmacoes: denuncia.resolucao?.confirmacoes ?? 0,
    status: denuncia.status?.codigo ?? '',
  };
}

/** Retrato salvo ({ [id]: retrato }) ou null na primeira visita. */
export function lerRetrato(usuarioId) {
  if (!usuarioId) return null;
  try {
    const salvo = JSON.parse(localStorage.getItem(chave(usuarioId)) ?? 'null');
    return salvo && typeof salvo === 'object' ? salvo : null;
  } catch {
    return null;
  }
}

/** Troca o retrato pelo estado atual da lista (denúncias apagadas saem junto). */
export function gravarRetrato(usuarioId, denuncias) {
  if (!usuarioId) return;
  try {
    const retrato = Object.fromEntries(denuncias.map((d) => [d.id, retratoDe(d)]));
    localStorage.setItem(chave(usuarioId), JSON.stringify(retrato));
  } catch {
    // sem armazenamento: só não haverá avisos na próxima visita
  }
}

/** ["+2 apoios", "+1 comentário", "status mudou para Resolvida"] (vazio se nada mudou). */
export function novidadesDa(anterior, denuncia) {
  if (!anterior) return []; // denúncia nova (ou primeira visita): nada para comparar
  const atual = retratoDe(denuncia);
  const textos = [];
  const subiu = (campo) => Math.max(0, atual[campo] - (Number(anterior[campo]) || 0));

  if (subiu('upvotes')) textos.push(`+${plural(subiu('upvotes'), 'apoio', 'apoios')}`);
  if (subiu('downvotes')) textos.push(`+${plural(subiu('downvotes'), 'discordância', 'discordâncias')}`);
  if (subiu('comentarios')) textos.push(`+${plural(subiu('comentarios'), 'comentário', 'comentários')}`);
  if (subiu('confirmacoes')) {
    textos.push(`+${plural(subiu('confirmacoes'), 'confirmação de resolvida', 'confirmações de resolvida')}`);
  }
  if (anterior.status && atual.status !== anterior.status) {
    textos.push(`status mudou para ${denuncia.status?.descricao ?? atual.status}`);
  }
  return textos;
}

/** { [id]: ["+2 apoios", ...] } só para as denúncias com novidades. */
export function novidadesPorDenuncia(usuarioId, denuncias) {
  const retrato = lerRetrato(usuarioId);
  if (!retrato) return {};
  const resultado = {};
  denuncias.forEach((d) => {
    const textos = novidadesDa(retrato[d.id], d);
    if (textos.length > 0) resultado[d.id] = textos;
  });
  return resultado;
}

/** Quantas denúncias têm novidades (para o Perfil). Não atualiza o retrato. */
export function contarComNovidades(usuarioId, denuncias) {
  return Object.keys(novidadesPorDenuncia(usuarioId, denuncias)).length;
}
