/**
 * Exportação do Painel da cidade em CSV, montada no próprio navegador a partir dos relatórios já
 * carregados (sem rota nova na API).
 *
 * Formato pensado para abrir direto no Excel e no Google Planilhas em português: separador ";",
 * vírgula decimal, quebra de linha CRLF e BOM no início (sem ele, o Excel lê os acentos errado).
 */

const SEPARADOR = ';';
const QUEBRA = '\r\n';

/** Um valor de célula: números com vírgula decimal; texto entre aspas quando tem ; " ou quebra de linha. */
export function celula(valor) {
  if (valor === null || valor === undefined) return '';
  if (typeof valor === 'number') return Number.isFinite(valor) ? String(valor).replace('.', ',') : '';
  const texto = String(valor);
  return /[;"\r\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto;
}

function linha(valores) {
  return valores.map(celula).join(SEPARADOR);
}

/**
 * @param dados  { resumo, categorias, bairros, engajamento } como o Painel guarda (listas em { valor } ou { erro })
 * @param agora  data da geração (parâmetro para o teste ser previsível)
 * @returns conteúdo do arquivo, já com o BOM
 */
export function montarCsvPainel(dados, agora = new Date()) {
  const { resumo } = dados;
  const geradoEm = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(agora);
  const linhas = [
    linha(['Painel da cidade', `${resumo.cidade}/${resumo.uf}`, `gerado em ${geradoEm}`]),
    linha(['Fonte', 'Meus Direitos (protótipo acadêmico)', resumo.denunciasSimuladas > 0
      ? `inclui ${resumo.denunciasSimuladas} denúncia(s) de demonstração`
      : 'só denúncias de moradores reais']),
    '',
    linha(['Resumo']),
    linha(['Indicador', 'Valor']),
    linha(['Denúncias', resumo.denuncias]),
    linha(['Em aberto', resumo.abertas]),
    linha(['Em andamento', resumo.emAndamento]),
    linha(['Resolvidas', resumo.resolvidas]),
    linha(['% resolvidas', resumo.percentualResolvidas]),
    linha(['Moradores', resumo.usuarios]),
    linha(['Votos', resumo.votos]),
    linha(['Comentários', resumo.comentarios]),
    linha(['Confirmações de resolvida', resumo.confirmacoesDeResolucao]),
  ];

  const categorias = dados.categorias?.valor ?? [];
  if (categorias.length > 0) {
    linhas.push('', linha(['Por categoria']),
      linha(['Categoria', 'Total', 'Em aberto', 'Em andamento', 'Resolvidas', '% resolvidas']));
    categorias.forEach((c) => linhas.push(linha([c.categoria?.descricao, c.total, c.abertas, c.emAndamento,
      c.resolvidas, c.percentualResolvidas])));
  }

  const bairros = dados.bairros?.valor ?? [];
  if (bairros.length > 0) {
    linhas.push('', linha(['Por bairro']), linha(['Bairro', 'Total', 'Resolvidas', '% resolvidas', 'Saldo de votos']));
    bairros.forEach((b) => linhas.push(linha([b.bairro, b.total, b.resolvidas, b.percentualResolvidas, b.saldoVotos])));
  }

  const engajadas = dados.engajamento?.valor ?? [];
  if (engajadas.length > 0) {
    linhas.push('', linha(['Denúncias com mais participação']),
      linha(['Título', 'Categoria', 'Situação', 'Votos', 'Saldo', 'Comentários']));
    engajadas.forEach((d) => linhas.push(linha([d.titulo, d.categoria?.descricao, d.status?.descricao, d.votos,
      d.saldo, d.comentarios])));
  }

  return `﻿${linhas.join(QUEBRA)}${QUEBRA}`;
}

/** "São José dos Campos", "SP" → "painel-sao-jose-dos-campos-sp-2026-11-15.csv" */
export function nomeArquivoPainel(cidade, uf, agora = new Date()) {
  const slug = `${cidade ?? 'cidade'} ${uf ?? ''}`
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
  const dia = [agora.getFullYear(), String(agora.getMonth() + 1).padStart(2, '0'), String(agora.getDate()).padStart(2, '0')].join('-');
  return `painel-${slug}-${dia}.csv`;
}

/** Faz o navegador baixar o texto como arquivo (funciona também com o app aberto por file://). */
export function baixarArquivo(nome, conteudo, tipo = 'text/csv;charset=utf-8') {
  const url = URL.createObjectURL(new Blob([conteudo], { type: tipo }));
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // dá tempo ao navegador de começar o download antes de liberar a memória
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
