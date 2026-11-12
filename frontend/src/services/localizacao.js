/**
 * Localização: usada na tela 32 (Canais oficiais: onde a pessoa está, o portal da prefeitura
 * dessa cidade e a busca por CRAS próximos) e na nova denúncia ("Usar minha localização":
 * rua, número e bairro a partir da posição).
 *
 * Serviços usados (públicos e sem chave):
 * - Geolocalização do navegador: pede permissão à pessoa.
 * - Nominatim (OpenStreetMap): transforma coordenadas em cidade e UF e, muitas vezes, já traz o site
 *   oficial do município. Política de uso: no máximo 1 pedido por segundo, por isso o resultado fica
 *   guardado na sessão.
 * - Wikidata: site oficial do município (propriedade P856) quando o OpenStreetMap não tem.
 * - Google Maps: busca "CRAS" em volta da posição (ou na cidade).
 */

const NOMINATIM = 'https://nominatim.openstreetmap.org';
const WIKIDATA = 'https://www.wikidata.org/w/api.php';
const CHAVE_CACHE = 'md:canais';
const TEMPO_LIMITE_MS = 10000;

export const PORTAL_GOV_BR = 'https://www.gov.br';

/** Posição atual da pessoa. Rejeita se ela negar, se não houver suporte ou se demorar demais. */
export function obterPosicao() {
  return new Promise((resolver, rejeitar) => {
    if (!('geolocation' in navigator)) {
      rejeitar(new Error('sem-suporte'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (posicao) => resolver({ lat: posicao.coords.latitude, lon: posicao.coords.longitude }),
      (erro) => rejeitar(new Error(erro.code === erro.PERMISSION_DENIED ? 'negada' : 'indisponivel')),
      { enableHighAccuracy: false, timeout: TEMPO_LIMITE_MS, maximumAge: 10 * 60 * 1000 },
    );
  });
}

async function buscarJson(url) {
  const controle = new AbortController();
  const timer = setTimeout(() => controle.abort(), TEMPO_LIMITE_MS);
  try {
    const resposta = await fetch(url, { signal: controle.signal, headers: { Accept: 'application/json' } });
    if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
    return await resposta.json();
  } finally {
    clearTimeout(timer);
  }
}

/** Resultado do Nominatim → { cidade, uf, site, wikidata, lat, lon } */
function lerLugar(lugar) {
  if (!lugar || !lugar.address) return null;
  const endereco = lugar.address;
  const cidade = endereco.city ?? endereco.town ?? endereco.municipality ?? endereco.village ?? lugar.name;
  const uf = (endereco['ISO3166-2-lvl4'] ?? '').replace('BR-', '') || null;
  return {
    cidade,
    uf,
    site: lugar.extratags?.website ?? lugar.extratags?.['contact:website'] ?? null,
    wikidata: lugar.extratags?.wikidata ?? null,
    lat: Number(lugar.lat),
    lon: Number(lugar.lon),
  };
}

/** Coordenadas → cidade e UF (com site oficial quando o OpenStreetMap tiver). */
export async function cidadePorPosicao({ lat, lon }) {
  const parametros = new URLSearchParams({
    format: 'jsonv2', lat, lon, zoom: 10, extratags: 1, addressdetails: 1, 'accept-language': 'pt-BR',
  });
  return lerLugar(await buscarJson(`${NOMINATIM}/reverse?${parametros}`));
}

/**
 * Coordenadas → endereço da nova denúncia: { endereco: "Rua, número", bairro }.
 * Zoom 18 = nível de edifício. Qualquer parte pode vir vazia (rua sem nome no mapa, sem número).
 */
export async function enderecoPorPosicao({ lat, lon }) {
  const parametros = new URLSearchParams({
    format: 'jsonv2', lat, lon, zoom: 18, addressdetails: 1, 'accept-language': 'pt-BR',
  });
  const lugar = await buscarJson(`${NOMINATIM}/reverse?${parametros}`);
  const partes = lugar?.address ?? {};
  const endereco = [partes.road, partes.house_number].filter(Boolean).join(', ');
  const bairro = partes.suburb ?? partes.neighbourhood ?? partes.city_district ?? '';
  return { endereco, bairro };
}

/** Nome da cidade e UF (do cadastro) → mesmos dados, quando não há localização. */
export async function cidadePorNome(cidade, uf) {
  const parametros = new URLSearchParams({
    city: cidade, state: uf, country: 'Brasil', format: 'jsonv2', extratags: 1, addressdetails: 1, limit: 1,
    'accept-language': 'pt-BR',
  });
  const lista = await buscarJson(`${NOMINATIM}/search?${parametros}`);
  return lerLugar(lista[0]) ?? null;
}

/** Site oficial do município no Wikidata (P856), ou null. */
async function siteNoWikidata(idWikidata) {
  if (!/^Q\d+$/.test(idWikidata ?? '')) return null;
  const parametros = new URLSearchParams({
    action: 'wbgetclaims', entity: idWikidata, property: 'P856', format: 'json', origin: '*',
  });
  const dados = await buscarJson(`${WIKIDATA}?${parametros}`);
  const declaracoes = dados.claims?.P856 ?? [];
  // prefere a declaração marcada como preferida; senão, a primeira
  const escolhida = declaracoes.find((d) => d.rank === 'preferred') ?? declaracoes[0];
  return escolhida?.mainsnak?.datavalue?.value ?? null;
}

/** Só aceita endereços http(s), para nunca abrir um esquema perigoso vindo de dados externos. */
function enderecoSeguro(url) {
  try {
    const endereco = new URL(url);
    return endereco.protocol === 'https:' || endereco.protocol === 'http:' ? endereco.href : null;
  } catch {
    return null;
  }
}

/** Busca na web pela prefeitura: último recurso quando não achamos o site oficial. */
export function buscaDaPrefeitura(cidade, uf) {
  const termo = `Prefeitura de ${cidade} ${uf ?? ''} site oficial`.trim();
  return `https://www.google.com/search?q=${encodeURIComponent(termo)}`;
}

/**
 * Portal da prefeitura do lugar: site do OpenStreetMap → site do Wikidata → busca na web.
 * @returns {{ url: string, oficial: boolean }}
 */
export async function portalDaPrefeitura(lugar) {
  const doMapa = enderecoSeguro(lugar.site);
  if (doMapa) return { url: doMapa, oficial: true };
  try {
    const doWikidata = enderecoSeguro(await siteNoWikidata(lugar.wikidata));
    if (doWikidata) return { url: doWikidata, oficial: true };
  } catch {
    // Wikidata fora do ar: cai na busca
  }
  return { url: buscaDaPrefeitura(lugar.cidade, lugar.uf), oficial: false };
}

/** CRAS próximos: no mapa em volta das coordenadas ou, sem elas, na cidade. */
export function buscaDeCras({ lat, lon, cidade, uf }) {
  if (Number.isFinite(lat) && Number.isFinite(lon)) {
    return `https://www.google.com/maps/search/CRAS/@${lat.toFixed(5)},${lon.toFixed(5)},14z`;
  }
  const termo = ['CRAS', cidade, uf].filter(Boolean).join(' ');
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(termo)}`;
}

/** Guarda o resultado na sessão: respeita o limite do Nominatim e evita pedir de novo a cada visita. */
export function lerCache(chave) {
  try {
    const salvo = JSON.parse(sessionStorage.getItem(CHAVE_CACHE) ?? 'null');
    return salvo?.chave === chave ? salvo.valor : null;
  } catch {
    return null;
  }
}

export function gravarCache(chave, valor) {
  try {
    sessionStorage.setItem(CHAVE_CACHE, JSON.stringify({ chave, valor }));
  } catch {
    // armazenamento indisponível: só não guarda
  }
}
