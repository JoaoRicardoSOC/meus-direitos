import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Progresso from '../../components/Progresso.jsx';
import Campo from '../../components/Campo.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { CampoEscolha, ListaOpcoes, PerguntaSimNao } from './QuestionarioCampos.jsx';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import './Questionario.css';

const TOTAL_ETAPAS = 3;

/** Campos de cada etapa: "Pular por enquanto" deixa todos eles sem resposta (null). */
const CAMPOS_DA_ETAPA = {
  1: ['dataNascimento', 'racaCor'],
  2: ['deficiencia', 'percursoEscolar'],
  3: ['baixaRenda'],
};

const SEM_RESPOSTA = { dataNascimento: '', racaCor: null, deficiencia: null, percursoEscolar: null, baixaRenda: null };

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

/** Data local no formato da API: "AAAA-MM-DD". */
function dataIso(data) {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

function hoje() {
  const data = new Date();
  data.setHours(0, 0, 0, 0);
  return data;
}

/** Mesma regra do back-end: opcional, no passado e de até 120 anos atrás. */
function validarNascimento(valor) {
  if (!valor) return '';
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor);
  if (!partes) return 'Informe uma data válida.';
  const [ano, mes, dia] = partes.slice(1).map(Number);
  const data = new Date(ano, mes - 1, dia);
  if (data.getFullYear() !== ano || data.getMonth() !== mes - 1 || data.getDate() !== dia) {
    return 'Informe uma data válida.';
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
    dataNascimento: perfil.dataNascimento ?? '',
    racaCor: perfil.racaCor?.codigo ?? null,
    deficiencia: perfil.deficiencia?.codigo ?? null,
    percursoEscolar: perfil.percursoEscolar?.codigo ?? null,
    baixaRenda: perfil.baixaRenda ?? null,
  };
}

function descricao(opcoes, codigo) {
  return opcoes.find((opcao) => opcao.codigo === codigo)?.descricao ?? null;
}

/**
 * 23 · Sobre você → 24 · Raça e cor → 25 · Acessibilidade e estudos → 26 · Deficiência → 27 · Escolaridade,
 * mais a etapa da renda familiar (pedido 4 do guia da API). Tudo é opcional.
 * As opções vêm da API; se a pessoa já respondeu, o formulário começa com as respostas atuais.
 * No fim, PUT /usuarios/perfil e volta para "Meus Direitos".
 */
export default function Questionario() {
  const navegar = useNavigate();
  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState('');
  const [tentativa, setTentativa] = useState(0);
  const [opcoes, setOpcoes] = useState({ racaCor: [], deficiencia: [], percursoEscolar: [] });
  const [valores, setValores] = useState(SEM_RESPOSTA);
  const [etapa, setEtapa] = useState(1);
  const [lista, setLista] = useState(null); // 'racaCor' | 'deficiencia' | 'percursoEscolar' enquanto escolhe
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);

  const camposEscolha = useRef({});
  const focarDepois = useRef(null);

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
        if (perfilCompleto?.perfil) setValores(respostasDoPerfil(perfilCompleto.perfil));
      })
      .catch((erro) => ativo && setErroCarga(erro.message))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [tentativa]);

  // Cada etapa e cada lista de opções começa no topo; ao voltar de uma lista, o foco volta ao campo dela.
  useEffect(() => {
    window.scrollTo(0, 0);
    if (!lista && focarDepois.current) {
      camposEscolha.current[focarDepois.current]?.focus();
      focarDepois.current = null;
    }
  }, [etapa, lista]);

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
  }

  function abrirLista(campo) {
    setMensagem('');
    setLista(campo);
  }

  function fecharLista(campo) {
    focarDepois.current = campo;
    setLista(null);
  }

  function escolher(campo, codigo) {
    mudar({ [campo]: codigo });
    fecharLista(campo);
  }

  function voltar() {
    setMensagem('');
    if (lista) fecharLista(lista);
    else if (etapa > 1) setEtapa(etapa - 1);
    else if (window.history.state?.idx > 0) navegar(-1);
    else navegar('/direitos');
  }

  function avancar(e) {
    e.preventDefault();
    setMensagem('');
    if (etapa === 1) {
      const erro = validarNascimento(valores.dataNascimento);
      if (erro) {
        setErros({ dataNascimento: erro });
        return;
      }
    }
    seguir(valores);
  }

  function pular() {
    setMensagem('');
    const vazios = Object.fromEntries(CAMPOS_DA_ETAPA[etapa].map((campo) => [campo, SEM_RESPOSTA[campo]]));
    const novos = { ...valores, ...vazios };
    mudar(vazios);
    seguir(novos);
  }

  function seguir(respostas) {
    if (etapa < TOTAL_ETAPAS) setEtapa(etapa + 1);
    else salvar(respostas);
  }

  async function salvar(respostas) {
    setEnviando(true);
    try {
      const perfilCompleto = await api('/usuarios/perfil', {
        metodo: 'PUT',
        corpo: { ...respostas, dataNascimento: respostas.dataNascimento || null },
      });
      navegar('/direitos', { replace: true, state: { perfilCompleto } });
    } catch (erro) {
      const porCampo = erro.porCampo();
      const etapaComErro = Object.entries(CAMPOS_DA_ETAPA)
        .find(([, campos]) => campos.some((campo) => porCampo[campo]))?.[0];
      setErros(porCampo);
      if (etapaComErro) setEtapa(Number(etapaComErro));
      setMensagem(erro.message);
    } finally {
      setEnviando(false);
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
      </Tela>
    );
  }

  const cabecalho = etapa === 1 ? 'Sobre você' : 'Sua realidade';
  const ultimaEtapa = etapa === TOTAL_ETAPAS;
  const ontem = hoje();
  ontem.setDate(ontem.getDate() - 1);
  const minimo = hoje();
  minimo.setFullYear(minimo.getFullYear() - 120);

  return (
    <Tela titulo={cabecalho}>
      <Cabecalho titulo={cabecalho} aoVoltar={voltar} />
      <Progresso etapa={etapa} total={TOTAL_ETAPAS} />

      <form className="md-questionario-formulario" onSubmit={avancar} noValidate>
        {/* 23 · Sobre você */}
        {etapa === 1 && (
          <>
            <h2 className="md-titulo">
              Cada história
              <br />
              é única.
            </h2>
            <p className="md-corpo">Estas informações ajudam a orientar a busca.</p>
            <Campo
              rotulo="Data de nascimento"
              type="date"
              value={valores.dataNascimento}
              onChange={(e) => mudar({ dataNascimento: e.target.value })}
              erro={erros.dataNascimento}
              min={dataIso(minimo)}
              max={dataIso(ontem)}
              autoComplete="bday"
            />
            <CampoEscolha
              ref={(el) => { camposEscolha.current.racaCor = el; }}
              rotulo="Como você se declara?"
              valor={descricao(opcoes.racaCor, valores.racaCor)}
              erro={erros.racaCor}
              aoAbrir={() => abrirLista('racaCor')}
            />
            <div className="md-informacao">
              <img src={iconeCadeado} alt="" width="20" height="20" />
              <p className="md-legenda">Raça/cor é uma informação opcional. Você pode seguir sem responder.</p>
            </div>
          </>
        )}

        {/* 25 · Acessibilidade e estudos */}
        {etapa === 2 && (
          <>
            <h2 className="md-titulo">
              Como podemos
              <br />
              apoiar você?
            </h2>
            <p className="md-corpo">Você pode revisar suas respostas depois.</p>
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
              <img src={iconeCadeado} alt="" width="20" height="20" />
              <p className="md-legenda">
                A deficiência, por si só, não confirma acesso a um benefício. Cada programa tem critérios próprios.
              </p>
            </div>
          </>
        )}

        {/* Renda familiar: pergunta pedida pelo back-end (não está no Figma) */}
        {etapa === 3 && (
          <>
            <h2 className="md-titulo">
              E a renda da
              <br />
              sua família?
            </h2>
            <p className="md-corpo">
              Alguns apoios, como BPC, Tarifa Social e ID Jovem, consideram a renda da família.
            </p>
            <PerguntaSimNao
              rotulo="Sua família está no CadÚnico ou recebe benefício social?"
              valor={valores.baixaRenda}
              erro={erros.baixaRenda}
              aoMudar={(baixaRenda) => mudar({ baixaRenda })}
            />
            <div className="md-informacao">
              <img src={iconeCadeado} alt="" width="20" height="20" />
              <p className="md-legenda">
                Esta resposta é opcional. Ela só serve para indicar possibilidades a verificar.
              </p>
            </div>
          </>
        )}

        <Mensagem>{mensagem}</Mensagem>

        <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
          {!ultimaEtapa ? 'Continuar' : enviando ? 'Buscando possibilidades…' : 'Ver possibilidades'}
        </button>
        <button type="button" className="md-botao md-botao-secundario" onClick={pular} disabled={enviando}>
          Pular por enquanto
        </button>
      </form>
    </Tela>
  );
}
