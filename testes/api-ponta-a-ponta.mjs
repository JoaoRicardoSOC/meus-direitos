// Teste de ponta a ponta da API: percorre o caminho de uso do app contra a API rodando (e o banco de verdade).
// Só precisa do Node 22 (fetch nativo), sem dependências.
//
//   node testes/api-ponta-a-ponta.mjs                       (API em http://localhost:8080/api)
//   node testes/api-ponta-a-ponta.mjs http://127.0.0.1:8080/api
//
// Grava apenas pelas rotas da API e, no final (mesmo se algo falhar), exclui as contas de teste
// pela própria API, o que apaga junto as denúncias, votos, comentários e reportes criados.

const API = (process.argv[2] ?? process.env.MD_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');
const sufixo = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
const SENHA = 'Teste2026api';
const CIDADE = { cidade: 'São Paulo', uf: 'SP' };

const resultados = [];
const contas = [];

async function chamar(metodo, caminho, { token, corpo } = {}) {
  const cabecalhos = {};
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (token) cabecalhos.Authorization = `Bearer ${token}`;
  const resposta = await fetch(`${API}${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const texto = await resposta.text();
  let dados = null;
  try {
    dados = texto ? JSON.parse(texto) : null;
  } catch {
    dados = texto;
  }
  return { status: resposta.status, dados, cabecalhos: resposta.headers };
}

const temErroNoCampo = (resposta, campo) =>
  Array.isArray(resposta.dados?.campos) && resposta.dados.campos.some((c) => c.campo === campo);

function verificar(nome, ok, detalhe = '') {
  resultados.push({ nome, ok: Boolean(ok) });
  console.log(`${ok ? 'OK    ' : 'FALHOU'} ${nome}${detalhe ? ` — ${detalhe}` : ''}`);
}

async function criarConta(rotulo) {
  const email = `teste.api.${rotulo}.${sufixo}@exemplo.com`;
  const r = await chamar('POST', '/usuarios', {
    corpo: { nome: `Teste API ${rotulo}`, email, senha: SENHA, ...CIDADE },
  });
  if (r.status !== 201 || !r.dados?.token) {
    throw new Error(`cadastro ${rotulo} → ${r.status} ${JSON.stringify(r.dados)}`);
  }
  const conta = { email, id: r.dados.id, token: r.dados.token };
  contas.push(conta);
  return conta;
}

try {
  // 1. Infra
  const saude = await chamar('GET', '/health');
  verificar('API e banco no ar', saude.status === 200 && saude.dados?.banco === 'UP', JSON.stringify(saude.dados));
  const categorias = await chamar('GET', '/categorias');
  verificar(
    'Lista fixa com cache público',
    categorias.status === 200 && /max-age/.test(categorias.cabecalhos.get('cache-control') ?? ''),
    categorias.cabecalhos.get('cache-control') ?? 'sem Cache-Control',
  );
  verificar('Cabeçalho nosniff', saude.cabecalhos.get('x-content-type-options') === 'nosniff');

  // 2. Cadastro devolve token; rota protegida exige o token
  const a = await criarConta('a');
  const b = await criarConta('b');
  verificar('Cadastro devolve token', Boolean(a.token && b.token));
  const semToken = await chamar('GET', '/usuarios/me');
  const tokenFalso = await chamar('GET', '/usuarios/me', { token: `${a.token.slice(0, -2)}xx` });
  verificar('Sem token ou com token adulterado → 401', semToken.status === 401 && tokenFalso.status === 401,
    `${semToken.status} / ${tokenFalso.status}`);
  const me = await chamar('GET', '/usuarios/me', { token: a.token });
  verificar('Token válido identifica a pessoa', me.status === 200 && me.dados?.id === a.id && !me.dados?.token);
  const pessoal = await chamar('GET', '/usuarios/me', { token: a.token });
  verificar('Rota pessoal com no-store', /no-store/.test(pessoal.cabecalhos.get('cache-control') ?? ''));

  // 3. Login
  const login = await chamar('POST', '/auth/login', { corpo: { email: a.email, senha: SENHA } });
  verificar('Login devolve token', login.status === 200 && Boolean(login.dados?.token));
  const errado = await chamar('POST', '/auth/login', { corpo: { email: a.email, senha: 'SenhaErrada1' } });
  verificar('Senha errada → 401', errado.status === 401);

  // 4. Feed
  const feed = await chamar('GET', '/denuncias/feed?tamanho=5', { token: a.token });
  verificar('Feed da cidade', feed.status === 200 && Array.isArray(feed.dados?.itens), `${feed.dados?.total ?? '?'} denúncias`);

  // 5. Nova denúncia (só fotos de demonstração)
  const denuncia = {
    titulo: 'Teste automatizado de buraco',
    descricao: 'Denúncia criada pelo teste de ponta a ponta da API.',
    categoria: 'ASFALTO',
    endereco: 'Rua do Teste, 100',
    bairro: 'Centro',
  };
  const fotoExterna = await chamar('POST', '/denuncias', {
    token: a.token, corpo: { ...denuncia, imagemUrl: 'https://exemplo.com/foto.jpg' },
  });
  verificar('Foto externa recusada (400)', fotoExterna.status === 400, JSON.stringify(fotoExterna.dados?.campos ?? ''));
  const criada = await chamar('POST', '/denuncias', {
    token: a.token, corpo: { ...denuncia, imagemUrl: 'img/demo/asfalto-1.jpg' },
  });
  verificar('Denúncia publicada com protocolo', criada.status === 201 && Boolean(criada.dados?.protocolo), criada.dados?.protocolo);
  const idDenuncia = criada.dados?.id;

  // 6. Busca pública encontra a denúncia
  const busca = await chamar('GET', `/denuncias?cidade=Sao%20Paulo&uf=SP&busca=${encodeURIComponent('Teste automatizado')}`);
  verificar('Busca sem acento encontra a denúncia', busca.dados?.itens?.some((d) => d.id === idDenuncia));

  // 7. Voto de outra pessoa
  const voto = await chamar('PUT', `/denuncias/${idDenuncia}/voto`, { token: b.token, corpo: { tipo: 'UP' } });
  verificar('Voto registrado', voto.status === 200 && voto.dados?.upvotes === 1, JSON.stringify(voto.dados));

  // 8. Comentários: termo ofensivo recusado; reporte; 3 reportes ocultam
  const comentario = await chamar('POST', `/denuncias/${idDenuncia}/comentarios`, {
    token: a.token, corpo: { texto: 'Comentário do teste automatizado.' },
  });
  verificar('Comentário criado', comentario.status === 201);
  const idComentario = comentario.dados?.id;
  const ofensivo = await chamar('POST', `/denuncias/${idDenuncia}/comentarios`, {
    token: b.token, corpo: { texto: 'Isso aqui é uma merda.' },
  });
  verificar('Comentário ofensivo recusado (400 no texto)', ofensivo.status === 400 && temErroNoCampo(ofensivo, 'texto'));
  const proprio = await chamar('POST', `/comentarios/${idComentario}/reporte`, { token: a.token });
  verificar('Reportar o próprio comentário → 422', proprio.status === 422);
  const c = await criarConta('c');
  const d = await criarConta('d');
  const reportes = [];
  for (const conta of [b, c, d]) {
    reportes.push((await chamar('POST', `/comentarios/${idComentario}/reporte`, { token: conta.token })).status);
  }
  const repetido = await chamar('POST', `/comentarios/${idComentario}/reporte`, { token: b.token });
  verificar('Reportes aceitos e idempotentes (204)', [...reportes, repetido.status].every((s) => s === 204), reportes.join(','));
  const lista = await chamar('GET', `/denuncias/${idDenuncia}/comentarios`, { token: a.token });
  const detalhe = await chamar('GET', `/denuncias/${idDenuncia}`);
  verificar('Comentário com 3 reportes some da conversa e do total',
    Array.isArray(lista.dados) && !lista.dados.some((x) => x.id === idComentario) && detalhe.dados?.comentarios === 0,
    `${lista.dados?.length} na lista; total ${detalhe.dados?.comentarios}`);

  // 9. Questionário exige consentimento quando há dado sensível
  const semConsentimento = await chamar('PUT', '/usuarios/perfil', { token: a.token, corpo: { baixaRenda: true } });
  verificar('Questionário sem consentimento → 400', semConsentimento.status === 400 && temErroNoCampo(semConsentimento, 'consentimento'));
  const comConsentimento = await chamar('PUT', '/usuarios/perfil', {
    token: a.token, corpo: { dataNascimento: '1958-03-12', baixaRenda: true, consentimento: true },
  });
  verificar('Questionário com consentimento salvo', comConsentimento.status === 200,
    (comConsentimento.dados?.tags ?? []).map((t) => t.codigo).join(', '));

  // 10. Painel da cidade
  const resumo = await chamar('GET', '/relatorios/resumo', { token: a.token });
  const porCategoria = await chamar('GET', '/relatorios/categorias', { token: a.token });
  verificar('Relatórios da cidade', resumo.status === 200 && porCategoria.status === 200, `${resumo.dados?.denuncias ?? '?'} denúncias`);

  // 11. Limite de tentativas de login (conta D: 5 erros e a 6ª tentativa, mesmo certa, é bloqueada)
  for (let i = 0; i < 5; i++) {
    await chamar('POST', '/auth/login', { corpo: { email: d.email, senha: 'SenhaErrada1' } });
  }
  const bloqueado = await chamar('POST', '/auth/login', { corpo: { email: d.email, senha: SENHA } });
  verificar('6ª tentativa bloqueada (429)', bloqueado.status === 429, bloqueado.dados?.mensagem);
} catch (erro) {
  verificar('Execução sem erro inesperado', false, erro.message);
} finally {
  // Limpeza: excluir as contas apaga denúncias, votos, comentários e reportes ligados a elas.
  let excluidas = 0;
  for (const conta of contas) {
    const r = await chamar('POST', '/usuarios/me/exclusao', { token: conta.token, corpo: { senha: SENHA } }).catch(() => null);
    if (r?.status === 204) excluidas++;
  }
  verificar('Contas de teste excluídas', excluidas === contas.length, `${excluidas}/${contas.length}`);
}

const falhas = resultados.filter((r) => !r.ok).length;
console.log(`\n${resultados.length - falhas}/${resultados.length} verificações OK`);
process.exitCode = falhas ? 1 : 0;
