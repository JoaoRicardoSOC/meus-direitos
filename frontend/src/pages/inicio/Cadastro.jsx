import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Progresso from '../../components/Progresso.jsx';
import Campo from '../../components/Campo.jsx';
import CampoSenha from '../../components/CampoSenha.jsx';
import CampoCidade from '../../components/CampoCidade.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { validar, validarCampos } from '../../utils/validacao.js';
import { focarPrimeiroErro, irParaOTopo } from '../../utils/foco.js';
import iconeCadeado from '../../assets/icones/cadeado.svg';
import './inicio.css';

/** Campos validados em cada etapa: a pessoa só avança com a etapa correta. */
const CAMPOS_DA_ETAPA = {
  1: ['nome'],
  2: ['cidade', 'uf'],
  3: ['email'],
  4: ['senha'],
};
const TOTAL_ETAPAS = 4;
const ETAPA_DO_EMAIL = 3;

/** Rascunho do cadastro na aba (nunca a senha): sobrevive a recarregar a página ou sair e voltar. */
const CHAVE_RASCUNHO = 'md-cadastro-rascunho';
const VAZIO = { nome: '', cidade: '', uf: '', email: '', senha: '' };

function lerRascunho() {
  try {
    const salvo = JSON.parse(sessionStorage.getItem(CHAVE_RASCUNHO) ?? '{}');
    return {
      ...VAZIO,
      nome: String(salvo.nome ?? ''),
      cidade: String(salvo.cidade ?? ''),
      uf: String(salvo.uf ?? ''),
      email: String(salvo.email ?? ''),
    };
  } catch {
    return { ...VAZIO };
  }
}

function salvarRascunho({ nome, cidade, uf, email }) {
  try {
    sessionStorage.setItem(CHAVE_RASCUNHO, JSON.stringify({ nome, cidade, uf, email }));
  } catch {
    // sem armazenamento (aba privada): o cadastro segue, só não é lembrado
  }
}

function limparRascunho() {
  try {
    sessionStorage.removeItem(CHAVE_RASCUNHO);
  } catch {
    // nada a limpar
  }
}

function errosDaEtapa(valores, numero) {
  const regras = Object.fromEntries(CAMPOS_DA_ETAPA[numero].map((campo) => [campo, validar[campo]]));
  return validarCampos(valores, regras);
}

function primeiroNome(nome) {
  return nome.trim().split(/\s+/)[0] ?? '';
}

/**
 * 02 · Nome → 03 · Cidade → 04 · E-mail → 05 · Senha.
 * A etapa fica na URL (/cadastro?passo=2): o "voltar" do celular volta uma etapa.
 * As respostas vão juntas no fim (POST /usuarios).
 */
export default function Cadastro() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();
  const [parametros, setParametros] = useSearchParams();
  const [valores, setValores] = useState(lerRascunho);
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const formulario = useRef(null);
  const tituloDaEtapa = useRef(null);
  const focarErroAoTrocar = useRef(false);

  // Etapa pedida na URL, limitada à primeira etapa que ainda não está completa
  const pedida = Math.min(Math.max(Number.parseInt(parametros.get('passo'), 10) || 1, 1), TOTAL_ETAPAS);
  let etapa = pedida;
  for (let n = 1; n < pedida; n += 1) {
    if (Object.keys(errosDaEtapa(valores, n)).length > 0) {
      etapa = n;
      break;
    }
  }

  // Direção da animação: compara com a etapa anterior (padrão do React para "valor anterior")
  const [etapaAnterior, setEtapaAnterior] = useState(etapa);
  const [direcao, setDirecao] = useState('frente');
  if (etapa !== etapaAnterior) {
    setEtapaAnterior(etapa);
    setDirecao(etapa > etapaAnterior ? 'frente' : 'tras');
  }

  // URL pedindo uma etapa sem as anteriores: corrige sem empilhar no histórico
  useEffect(() => {
    if (etapa !== pedida) setParametros({ passo: String(etapa) }, { replace: true });
  }, [etapa, pedida, setParametros]);

  useEffect(() => {
    salvarRascunho(valores);
  }, [valores]);

  // A cada etapa: topo, na hora, e foco no título da pergunta (ou no campo recusado pela API)
  const primeiraEtapa = useRef(true);
  useEffect(() => {
    if (primeiraEtapa.current) primeiraEtapa.current = false;
    else irParaOTopo();
    // dois quadros: depois do foco que o GerenciarRota/irParaOTopo põem no título do cabeçalho
    const quadro = requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (focarErroAoTrocar.current) {
          focarErroAoTrocar.current = false;
          focarPrimeiroErro(formulario.current);
        } else {
          tituloDaEtapa.current?.focus({ preventScroll: true });
        }
      });
    });
    return () => cancelAnimationFrame(quadro);
    // local.key: também quando a URL é corrigida para a etapa certa (o GerenciarRota foca o cabeçalho)
  }, [etapa, local.key]);

  function irParaEtapa(numero, opcoes = {}) {
    setMensagem('');
    setParametros({ passo: String(numero) }, { state: { etapaDoCadastro: true }, ...opcoes });
  }

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
  }

  function voltar() {
    setMensagem('');
    if (etapa === 1) navegar('/');
    // chegou aqui avançando pelo cadastro: volta no histórico, como o "voltar" do celular
    else if (local.state?.etapaDoCadastro) navegar(-1);
    else irParaEtapa(etapa - 1, { replace: true });
  }

  async function avancar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = errosDaEtapa(valores, etapa);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) {
      setMensagem(Object.keys(encontrados).length > 1 ? 'Revise os campos destacados.' : 'Revise o campo destacado.');
      focarPrimeiroErro(formulario.current);
      return;
    }

    if (etapa < TOTAL_ETAPAS) {
      irParaEtapa(etapa + 1);
      return;
    }
    await finalizar();
  }

  async function finalizar() {
    setEnviando(true);
    try {
      const usuario = await api('/usuarios', { metodo: 'POST', corpo: valores });
      limparRascunho();
      entrar(usuario);
      navegar('/cidade', { replace: true, state: { novoCadastro: true, cidadePreparada: usuario.cidadePreparada } });
    } catch (erro) {
      // e-mail já usado (409) volta para a etapa do e-mail; erros de campo voltam para a etapa do campo
      const porCampo = erro.status === 409 ? { email: erro.message } : erro.porCampo();
      const etapaComErro = Number(
        Object.entries(CAMPOS_DA_ETAPA).find(([, campos]) => campos.some((campo) => porCampo[campo]))?.[0],
      );
      setErros(porCampo);
      if (etapaComErro && etapaComErro !== etapa) {
        focarErroAoTrocar.current = true;
        irParaEtapa(etapaComErro, { replace: true });
      } else if (etapaComErro) {
        focarPrimeiroErro(formulario.current);
      }
      if (erro.status !== 409) setMensagem(erro.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Criar conta">
      <Cabecalho titulo="Vamos nos conhecer" aoVoltar={voltar} />
      <Progresso etapa={etapa} total={TOTAL_ETAPAS} />

      <form ref={formulario} className="md-formulario-inicio" onSubmit={avancar} noValidate>
        <div key={etapa} className="md-etapa" data-direcao={direcao}>
          {etapa === 1 && (
            <>
              <h2 className="md-titulo" ref={tituloDaEtapa} tabIndex={-1} data-foco-tela>
                Para começarmos,
                <br />
                qual é o seu nome?
              </h2>
              <p className="md-corpo">Queremos saber como chamar você.</p>
              <Campo
                className="md-grupo"
                rotulo="Seu nome"
                value={valores.nome}
                onChange={(e) => mudar({ nome: e.target.value })}
                erro={erros.nome}
                autoComplete="name"
                placeholder="Ex.: Maria da Silva"
                maxLength={100}
              />
            </>
          )}

          {etapa === 2 && (
            <>
              <h2 className="md-titulo" ref={tituloDaEtapa} tabIndex={-1} data-foco-tela>
                Onde você mora,
                <br />
                {primeiroNome(valores.nome)}?
              </h2>
              <p className="md-corpo">Vamos aproximar você do seu bairro.</p>
              <div className="md-grupo">
                <CampoCidade cidade={valores.cidade} uf={valores.uf} aoMudar={mudar} erros={erros} />
              </div>
            </>
          )}

          {etapa === ETAPA_DO_EMAIL && (
            <>
              <h2 className="md-titulo" ref={tituloDaEtapa} tabIndex={-1} data-foco-tela>
                Qual é o seu
                <br />
                e-mail?
              </h2>
              <p className="md-corpo">Ele será usado para acessar sua conta.</p>
              <Campo
                className="md-grupo"
                rotulo="E-mail"
                type="email"
                value={valores.email}
                onChange={(e) => mudar({ email: e.target.value })}
                erro={erros.email}
                autoComplete="email"
                placeholder="Ex.: maria@email.com"
                maxLength={120}
              />
            </>
          )}

          {etapa === 4 && (
            <>
              <h2 className="md-titulo" ref={tituloDaEtapa} tabIndex={-1} data-foco-tela>
                Agora, crie uma
                <br />
                senha segura.
              </h2>
              <p className="md-corpo">Use pelo menos 8 caracteres, com letras e números.</p>
              <div className="md-grupo">
                <CampoSenha
                  value={valores.senha}
                  onChange={(e) => mudar({ senha: e.target.value })}
                  erro={erros.senha}
                  autoComplete="new-password"
                  maxLength={64}
                />
              </div>
            </>
          )}
        </div>

        <Mensagem>{mensagem}</Mensagem>

        <button type="submit" className="md-botao md-botao-primario md-grupo" disabled={enviando}>
          {etapa < TOTAL_ETAPAS ? 'Continuar' : enviando ? 'Preparando sua cidade…' : 'Finalizar cadastro'}
        </button>

        {etapa === TOTAL_ETAPAS && (
          <div className="md-informacao">
            <img src={iconeCadeado} alt="" width="20" height="20" />
            <p className="md-legenda">No protótipo, usamos apenas dados fictícios.</p>
          </div>
        )}
      </form>
    </Tela>
  );
}
