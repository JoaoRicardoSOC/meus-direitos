import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Progresso from '../../components/Progresso.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { lerUsuarioId } from '../../services/sessao.js';
import { focarPrimeiroErro, irParaOTopo } from '../../utils/foco.js';
import { CampoData, CampoEscolha, ListaOpcoes, PerguntaSimNao } from './QuestionarioCampos.jsx';
import iconeInfo from '../../assets/icones/info.svg';
import './Questionario.css';

const TOTAL_ETAPAS = 3;

/** Campos de cada etapa (nomes da API): usados para levar a pessoa à etapa de um erro devolvido no envio. */
const CAMPOS_DA_ETAPA = {
  1: ['dataNascimento', 'racaCor'],
  2: ['deficiencia', 'percursoEscolar'],
  3: ['baixaRenda'],
};

const DATA_VAZIA = { dia: '', mes: '', ano: '' };

const SEM_RESPOSTA = { nascimento: DATA_VAZIA, racaCor: null, deficiencia: null, percursoEscolar: null, baixaRenda: null };

/** Lista aberta na URL (&lista=) ↔ campo do formulário, e a etapa em que cada lista pode ser aberta. */
const LISTA_NA_URL = { raca: 'racaCor', deficiencia: 'deficiencia', percurso: 'percursoEscolar' };
const URL_DA_LISTA = { racaCor: 'raca', deficiencia: 'deficiencia', percursoEscolar: 'percurso' };
const ETAPA_DA_LISTA = { racaCor: 1, deficiencia: 2, percursoEscolar: 2 };

/** Telas de opções (24, 26 e 27): cabeçalho, título e explicação de cada uma. */
const LISTAS = {
  racaCor: { cabecalho: 'Como você se declara?', texto: 'Esta resposta é opcional.' },
  deficiencia: { cabecalho: 'Possui alguma deficiência?', texto: 'Selecione uma opção para a demonstração.' },
  percursoEscolar: {
    cabecalho: 'Sua escolaridade',
    titulo: (
      <>
        Onde você
        <br />
        estudou?
      </>
    ),
    texto: 'Selecione a opção do seu percurso escolar.',
  },
};

/** Respostas em andamento ficam na sessão da aba: reabrir o questionário retoma de onde parou. */
function chaveRascunho() {
  return `md-questionario-rascunho-${lerUsuarioId() ?? 'anonimo'}`;
}

function lerRascunho() {
  try {
    const salvo = JSON.parse(sessionStorage.getItem(chaveRascunho()) ?? 'null');
    if (!salvo || typeof salvo !== 'object') return null;
    return { ...SEM_RESPOSTA, ...salvo, nascimento: { ...DATA_VAZIA, ...(salvo.nascimento ?? {}) } };
  } catch {
    return null;
  }
}

function gravarRascunho(valores) {
  try {
    sessionStorage.setItem(chaveRascunho(), JSON.stringify(valores));
  } catch {
    // sem armazenamento (aba privada): o questionário segue funcionando, só não retoma
  }
}

function apagarRascunho() {
  try {
    sessionStorage.removeItem(chaveRascunho());
  } catch {
    // idem
  }
}

function hoje() {
  const data = new Date();
  data.setHours(0, 0, 0, 0);
  return data;
}

/** "AAAA-MM-DD" da API → { dia, mes, ano } dos três campos. */
function partesDaData(iso) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  if (!partes) return DATA_VAZIA;
  return { dia: String(Number(partes[3])), mes: partes[2], ano: partes[1] };
}

/** { dia, mes, ano } → "AAAA-MM-DD" (vazio quando os três estão em branco). */
function dataIso({ dia, mes, ano }) {
  if (!dia && !mes && !ano) return '';
  return `${ano}-${mes}-${dia.padStart(2, '0')}`;
}

/** Mesma regra do back-end: opcional, data real, no passado e de até 120 anos atrás. */
function validarNascimento(partesData) {
  const { dia, mes, ano } = partesData;
  if (!dia && !mes && !ano) return '';
  if (!dia || !mes || !ano) return 'Preencha dia, mês e ano, ou deixe os três em branco.';
  const numeroDia = Number(dia);
  if (numeroDia < 1 || numeroDia > 31) return 'Informe um dia de 1 a 31.';
  if (ano.length !== 4) return 'Informe o ano com 4 dígitos.';
  const [a, m, d] = [Number(ano), Number(mes), numeroDia];
  const data = new Date(a, m - 1, d);
  if (data.getFullYear() !== a || data.getMonth() !== m - 1 || data.getDate() !== d) {
    return 'Esta data não existe. Confira o dia e o mês.';
  }
  const limite = hoje();
  if (data >= limite) return 'A data de nascimento precisa estar no passado.';
  limite.setFullYear(limite.getFullYear() - 120);
  if (data < limite) return 'Informe uma data de no máximo 120 anos atrás.';
  return '';
}

/** Respostas atuais (GET /usuarios/perfil) no formato do formulário. */
function respostasDoPerfil(perfil) {
  return {
    nascimento: partesDaData(perfil.dataNascimento),
    racaCor: perfil.racaCor?.codigo ?? null,
    deficiencia: perfil.deficiencia?.codigo ?? null,
    percursoEscolar: perfil.percursoEscolar?.codigo ?? null,
    baixaRenda: perfil.baixaRenda ?? null,
  };
}

function descricao(opcoes, codigo) {
  return opcoes.find((opcao) => opcao.codigo === codigo)?.descricao ?? null;
}

/** Depois do foco que GerenciarRota/irParaOTopo dão ao título da tela (próximo quadro). */
function depoisDaTroca(funcao) {
  requestAnimationFrame(() => requestAnimationFrame(funcao));
}

/**
 * 23 · Sobre você → 24 · Raça e cor → 25 · Acessibilidade e estudos → 26 · Deficiência → 27 · Escolaridade,
 * mais a etapa da renda familiar (pedido 4 do guia da API). Tudo é opcional.
 * A etapa e a lista aberta ficam na URL (?passo=1..3&lista=raca|deficiencia|percurso): o "voltar" do
 * celular volta uma etapa ou fecha a lista. As opções vêm da API; se a pessoa já respondeu, o formulário
 * começa com as respostas atuais (ou com o rascunho desta aba). No fim, PUT /usuarios/perfil.
 */
export default function Questionario() {
  const navegar = useNavigate();
  const local = useLocation();
  const [parametros, setParametros] = useSearchParams();

  // Etapa e lista vêm da URL; o que for inválido vale como etapa 1 / sem lista (e a URL é corrigida abaixo)
  const passoNaUrl = Number(parametros.get('passo'));
  const etapa = Number.isInteger(passoNaUrl) && passoNaUrl >= 1 && passoNaUrl <= TOTAL_ETAPAS ? passoNaUrl : 1;
  const campoNaUrl = LISTA_NA_URL[parametros.get('lista')];
  const lista = campoNaUrl && ETAPA_DA_LISTA[campoNaUrl] === etapa ? campoNaUrl : null;
  /** Esta entrada do histórico foi empilhada pelo próprio questionário: "voltar" = desempilhar. */
  const empilhada = local.state?.questionario === true;
  /**
   * Posição no histórico (history.state.idx do React Router) da primeira tela do questionário.
   * Vai junto em cada etapa empilhada, para, ao salvar, sair de uma vez sem deixar etapas para trás.
   */
  const base = empilhada && Number.isInteger(local.state?.base) ? local.state.base : (window.history.state?.idx ?? 0);

  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState('');
  const [tentativa, setTentativa] = useState(0);
  const [opcoes, setOpcoes] = useState({ racaCor: [], deficiencia: [], percursoEscolar: [] });
  const [valores, setValores] = useState(SEM_RESPOSTA);
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Direção da entrada do conteúdo: avançar (ou abrir lista) entra pela direita; voltar, pela esquerda
  const posicao = etapa * 2 + (lista ? 1 : 0);
  const [posicaoAnterior, setPosicaoAnterior] = useState(posicao);
  const [direcao, setDirecao] = useState('frente');
  if (posicao !== posicaoAnterior) {
    setPosicaoAnterior(posicao);
    setDirecao(posicao < posicaoAnterior ? 'tras' : 'frente');
  }

  const formulario = useRef(null);
  const tituloEtapa = useRef(null);
  const camposEscolha = useRef({});
  const listaAnterior = useRef(lista);
  const primeiraPosicao = useRef(true);
  const focarErroDepois = useRef(false);
  const alterado = useRef(false);

  // URL fora do formato (sem ?passo, passo 7, lista de outra etapa…): corrige sem criar entrada no histórico
  useEffect(() => {
    const certo = lista ? { passo: String(etapa), lista: URL_DA_LISTA[lista] } : { passo: String(etapa) };
    if (parametros.toString() !== new URLSearchParams(certo).toString()) {
      setParametros(certo, { replace: true, state: local.state });
    }
  }, [parametros]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setErroCarga('');
    Promise.all([
      api('/racas-cor'),
      api('/deficiencias'),
      api('/percursos-escolares'),
      api('/usuarios/perfil').catch((erro) => {
        if (erro.status === 404) return null; // ainda não respondeu: questionário novo
        throw erro;
      }),
    ])
      .then(([racas, deficiencias, percursos, perfilCompleto]) => {
        if (!ativo) return;
        setOpcoes({ racaCor: racas ?? [], deficiencia: deficiencias ?? [], percursoEscolar: percursos ?? [] });
        const rascunho = lerRascunho();
        if (rascunho) setValores(rascunho);
        else if (perfilCompleto?.perfil) setValores(respostasDoPerfil(perfilCompleto.perfil));
      })
      .catch((erro) => ativo && setErroCarga(erro.message))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [tentativa]);

  // Só grava o rascunho depois que a pessoa mexe em alguma resposta
  useEffect(() => {
    if (alterado.current) gravarRascunho(valores);
  }, [valores]);

  // A cada etapa ou lista: topo da página e foco no título; ao fechar uma lista, o foco volta ao campo dela
  useEffect(() => {
    const veioDaLista = listaAnterior.current;
    listaAnterior.current = lista;
    if (primeiraPosicao.current) {
      primeiraPosicao.current = false;
      return; // ao abrir, GerenciarRota já cuida do topo e do foco
    }
    if (!lista && veioDaLista && ETAPA_DA_LISTA[veioDaLista] === etapa) {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      depoisDaTroca(() => camposEscolha.current[veioDaLista]?.focus());
      return;
    }
    irParaOTopo();
    if (focarErroDepois.current) {
      focarErroDepois.current = false;
      depoisDaTroca(() => focarPrimeiroErro(formulario.current));
    } else if (!lista) {
      depoisDaTroca(() => tituloEtapa.current?.focus({ preventScroll: true }));
    }
  }, [posicao]); // eslint-disable-line react-hooks/exhaustive-deps

  function irPara(passo, listaUrl) {
    const destino = listaUrl ? { passo: String(passo), lista: listaUrl } : { passo: String(passo) };
    setParametros(destino, { state: { questionario: true, base } });
  }

  /** Volta uma posição: desempilha se fomos nós que empilhamos; senão (link direto), troca a URL. */
  function voltarUmaPosicao(destino) {
    if (empilhada) navegar(-1);
    else setParametros({ passo: String(destino) }, { replace: true });
  }

  function mudar(parcial) {
    alterado.current = true;
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo === 'nascimento' ? 'dataNascimento' : campo]);
      return copia;
    });
  }

  function abrirLista(campo) {
    setMensagem('');
    irPara(etapa, URL_DA_LISTA[campo]);
  }

  function escolher(campo, codigo) {
    mudar({ [campo]: codigo });
    voltarUmaPosicao(etapa);
  }

  function voltar() {
    setMensagem('');
    if (lista) voltarUmaPosicao(etapa);
    else if (etapa > 1) voltarUmaPosicao(etapa - 1);
    else if (window.history.state?.idx > 0) navegar(-1);
    else navegar('/direitos');
  }

  function avancar(e) {
    e.preventDefault();
    setMensagem('');
    if (etapa === 1) {
      const erro = validarNascimento(valores.nascimento);
      if (erro) {
        setErros((atual) => ({ ...atual, dataNascimento: erro }));
        setMensagem('Revise a data de nascimento para continuar.');
        focarPrimeiroErro(formulario.current);
        return;
      }
    }
    seguir();
  }

  /** "Pular esta etapa": segue sem validar, mantendo o que já foi respondido nela. */
  function pular() {
    setMensagem('');
    seguir();
  }

  function seguir() {
    if (etapa < TOTAL_ETAPAS) irPara(etapa + 1);
    else salvar();
  }

  /** Leva a pessoa à etapa do primeiro erro, com o resumo e o foco no campo. */
  function mostrarErros(porCampo, resumo) {
    const etapaComErro = Number(
      Object.entries(CAMPOS_DA_ETAPA).find(([, campos]) => campos.some((campo) => porCampo[campo]))?.[0],
    );
    setErros(porCampo);
    setMensagem(resumo);
    if (!etapaComErro) return;
    if (etapaComErro === etapa) {
      focarPrimeiroErro(formulario.current);
    } else {
      focarErroDepois.current = true;
      setParametros({ passo: String(etapaComErro) }, { replace: true, state: local.state });
    }
  }

  async function salvar() {
    // A data pode ter ficado incompleta se a etapa 1 foi pulada
    const erroData = validarNascimento(valores.nascimento);
    if (erroData) {
      mostrarErros({ dataNascimento: erroData }, 'Revise a data de nascimento na etapa 1 antes de enviar.');
      return;
    }
    setEnviando(true);
    try {
      const { nascimento, ...resto } = valores;
      const perfilCompleto = await api('/usuarios/perfil', {
        metodo: 'PUT',
        corpo: { ...resto, dataNascimento: dataIso(nascimento) || null },
      });
      apagarRascunho();
      sairDoQuestionario(perfilCompleto);
    } catch (erro) {
      mostrarErros(erro.porCampo(), erro.message);
    } finally {
      setEnviando(false);
    }
  }

  /**
   * Depois de salvar, tira todas as etapas do histórico: o "voltar" em Meus Direitos não reabre o
   * questionário. Se havia uma tela do app antes dele, volta até ela e garante que é Meus Direitos;
   * se o questionário foi aberto por link direto, volta à primeira etapa e a troca por Meus Direitos.
   */
  function sairDoQuestionario(perfilCompleto) {
    const etapasEmpilhadas = Math.max(0, (window.history.state?.idx ?? 0) - base);
    const irParaMeusDireitos = () => navegar('/direitos', { replace: true, state: { perfilCompleto } });

    if (base > 0) {
      window.addEventListener(
        'popstate',
        () => setTimeout(() => {
          // a tela anterior costuma ser Meus Direitos; se não for, troca por ela
          if (window.location.hash.split('?')[0] !== '#/direitos') window.location.replace('#/direitos');
        }, 0),
        { once: true },
      );
      window.history.go(-(etapasEmpilhadas + 1));
    } else if (etapasEmpilhadas > 0) {
      window.addEventListener('popstate', () => setTimeout(irParaMeusDireitos, 0), { once: true });
      window.history.go(-etapasEmpilhadas);
    } else {
      irParaMeusDireitos();
    }
  }

  if (carregando || erroCarga) {
    return (
      <Tela titulo="Sobre você">
        <Cabecalho titulo="Sobre você" aoVoltar={voltar} />
        {carregando ? (
          <Carregando texto="Carregando perguntas…" />
        ) : (
          <>
            <Mensagem>{erroCarga}</Mensagem>
            <button type="button" className="md-botao md-botao-secundario" onClick={() => setTentativa((t) => t + 1)}>
              Tentar novamente
            </button>
          </>
        )}
      </Tela>
    );
  }

  // 24 · Raça e cor, 26 · Deficiência e 27 · Escolaridade
  if (lista) {
    const tela = LISTAS[lista];
    return (
      <Tela titulo={tela.cabecalho}>
        <Cabecalho titulo={tela.cabecalho} aoVoltar={voltar} />
        <div key={posicao} className="md-etapa" data-direcao={direcao}>
          {tela.titulo && <h2 className="md-titulo">{tela.titulo}</h2>}
          <p id="questionario-lista-texto" className="md-corpo">
            {tela.texto}
          </p>
          <ListaOpcoes
            opcoes={opcoes[lista]}
            selecionada={valores[lista]}
            rotuladaPor="questionario-lista-texto"
            aoEscolher={(codigo) => escolher(lista, codigo)}
          />
        </div>
      </Tela>
    );
  }

  const cabecalho = etapa === 1 ? 'Sobre você' : 'Sua realidade';
  const ultimaEtapa = etapa === TOTAL_ETAPAS;

  return (
    <Tela titulo={cabecalho}>
      <Cabecalho titulo={cabecalho} aoVoltar={voltar} />
      <Progresso etapa={etapa} total={TOTAL_ETAPAS} />

      <div key={posicao} className="md-etapa" data-direcao={direcao}>
        <form ref={formulario} className="md-questionario-formulario" onSubmit={avancar} noValidate>
          {/* 23 · Sobre você */}
          {etapa === 1 && (
            <>
              <div className="md-questionario-bloco">
                <h2 ref={tituloEtapa} className="md-titulo" tabIndex={-1}>
                  Cada história
                  <br />
                  é única.
                </h2>
                <p className="md-corpo">Estas informações ajudam a orientar a busca.</p>
              </div>
              <div className="md-questionario-bloco md-grupo">
                <CampoData
                  legenda="Data de nascimento"
                  valor={valores.nascimento}
                  erro={erros.dataNascimento}
                  aoMudar={(nascimento) => mudar({ nascimento })}
                />
                <CampoEscolha
                  ref={(el) => { camposEscolha.current.racaCor = el; }}
                  rotulo="Como você se declara?"
                  valor={descricao(opcoes.racaCor, valores.racaCor)}
                  erro={erros.racaCor}
                  aoAbrir={() => abrirLista('racaCor')}
                />
                <div className="md-informacao">
                  <img src={iconeInfo} alt="" width="20" height="20" />
                  <p className="md-legenda">Raça/cor é uma informação opcional. Você pode seguir sem responder.</p>
                </div>
              </div>
            </>
          )}

          {/* 25 · Acessibilidade e estudos */}
          {etapa === 2 && (
            <>
              <div className="md-questionario-bloco">
                <h2 ref={tituloEtapa} className="md-titulo" tabIndex={-1}>
                  Como podemos
                  <br />
                  apoiar você?
                </h2>
                <p className="md-corpo">Você pode revisar suas respostas depois.</p>
              </div>
              <div className="md-questionario-bloco md-grupo">
                <CampoEscolha
                  ref={(el) => { camposEscolha.current.deficiencia = el; }}
                  rotulo="Possui alguma deficiência?"
                  valor={descricao(opcoes.deficiencia, valores.deficiencia)}
                  erro={erros.deficiencia}
                  aoAbrir={() => abrirLista('deficiencia')}
                />
                <CampoEscolha
                  ref={(el) => { camposEscolha.current.percursoEscolar = el; }}
                  rotulo="Onde concluiu os estudos?"
                  valor={descricao(opcoes.percursoEscolar, valores.percursoEscolar)}
                  erro={erros.percursoEscolar}
                  aoAbrir={() => abrirLista('percursoEscolar')}
                />
                <div className="md-informacao">
                  <img src={iconeInfo} alt="" width="20" height="20" />
                  <p className="md-legenda">
                    A deficiência, por si só, não confirma acesso a um benefício. Cada programa tem critérios próprios.
                  </p>
                </div>
              </div>
            </>
          )}

          {/* Renda familiar: pergunta pedida pelo back-end (não está no Figma) */}
          {etapa === 3 && (
            <>
              <div className="md-questionario-bloco">
                <h2 ref={tituloEtapa} className="md-titulo" tabIndex={-1}>
                  E a renda da
                  <br />
                  sua família?
                </h2>
                <p className="md-corpo">
                  Alguns apoios, como BPC, Tarifa Social e ID Jovem, consideram a renda da família.
                </p>
              </div>
              <div className="md-questionario-bloco md-grupo">
                <PerguntaSimNao
                  rotulo="Sua família está no CadÚnico ou recebe benefício social?"
                  valor={valores.baixaRenda}
                  erro={erros.baixaRenda}
                  aoMudar={(baixaRenda) => mudar({ baixaRenda })}
                />
                <div className="md-informacao">
                  <img src={iconeInfo} alt="" width="20" height="20" />
                  <p className="md-legenda">
                    Esta resposta é opcional. Ela só serve para indicar possibilidades a verificar.
                  </p>
                </div>
              </div>
            </>
          )}

          <div className="md-questionario-bloco md-questionario-acoes md-grupo">
            <Mensagem>{mensagem}</Mensagem>
            <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
              {!ultimaEtapa ? 'Continuar' : enviando ? 'Buscando possibilidades…' : 'Ver possibilidades'}
            </button>
            <button type="button" className="md-botao md-botao-secundario" onClick={pular} disabled={enviando}>
              Pular esta etapa
            </button>
          </div>
        </form>
      </div>
    </Tela>
  );
}
