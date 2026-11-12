import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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

function primeiroNome(nome) {
  return nome.trim().split(/\s+/)[0] ?? '';
}

/**
 * 02 · Nome → 03 · Cidade → 04 · E-mail → 05 · Senha.
 * As respostas ficam no estado da tela e vão juntas no fim (POST /usuarios).
 */
export default function Cadastro() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const [etapa, setEtapa] = useState(1);
  const [valores, setValores] = useState({ nome: '', cidade: '', uf: '', email: '', senha: '' });
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
  }

  function validarEtapa(numero) {
    const regras = Object.fromEntries(CAMPOS_DA_ETAPA[numero].map((campo) => [campo, validar[campo]]));
    return validarCampos(valores, regras);
  }

  function voltar() {
    setMensagem('');
    if (etapa === 1) navegar('/');
    else setEtapa(etapa - 1);
  }

  async function avancar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = validarEtapa(etapa);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;

    if (etapa < TOTAL_ETAPAS) {
      setEtapa(etapa + 1);
      return;
    }
    await finalizar();
  }

  async function finalizar() {
    setEnviando(true);
    try {
      const usuario = await api('/usuarios', { metodo: 'POST', corpo: valores });
      entrar(usuario);
      navegar('/cidade', { replace: true, state: { novoCadastro: true, cidadePreparada: usuario.cidadePreparada } });
    } catch (erro) {
      // e-mail já usado (409) volta para a etapa do e-mail; erros de campo voltam para a etapa do campo
      const porCampo = erro.status === 409 ? { email: erro.message } : erro.porCampo();
      const etapaComErro = Object.entries(CAMPOS_DA_ETAPA)
        .find(([, campos]) => campos.some((campo) => porCampo[campo]))?.[0];
      setErros(porCampo);
      if (etapaComErro) setEtapa(Number(etapaComErro));
      setMensagem(erro.status === 409 ? '' : erro.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Criar conta">
      <Cabecalho titulo="Vamos nos conhecer" aoVoltar={voltar} />
      <div className="md-espaco-cabecalho" />
      <Progresso etapa={etapa} total={TOTAL_ETAPAS} />
      <div className="md-espaco-titulo" />

      <form className="d-flex flex-column gap-3" onSubmit={avancar} noValidate>
        {etapa === 1 && (
          <>
            <h2 className="md-titulo">
              Para começarmos,
              <br />
              qual é o seu nome?
            </h2>
            <p className="md-corpo">Queremos saber como chamar você.</p>
            <Campo
              rotulo="Seu nome"
              value={valores.nome}
              onChange={(e) => mudar({ nome: e.target.value })}
              erro={erros.nome}
              autoComplete="name"
              placeholder="Mariana Oliveira"
              maxLength={100}
              autoFocus
            />
          </>
        )}

        {etapa === 2 && (
          <>
            <h2 className="md-titulo">
              Onde você mora,
              <br />
              {primeiroNome(valores.nome)}?
            </h2>
            <p className="md-corpo">Vamos aproximar você do seu bairro.</p>
            <CampoCidade cidade={valores.cidade} uf={valores.uf} aoMudar={mudar} erros={erros} />
          </>
        )}

        {etapa === 3 && (
          <>
            <h2 className="md-titulo">
              Qual é o seu
              <br />
              e-mail?
            </h2>
            <p className="md-corpo">Ele será usado para acessar sua conta.</p>
            <Campo
              rotulo="E-mail"
              type="email"
              value={valores.email}
              onChange={(e) => mudar({ email: e.target.value })}
              erro={erros.email}
              autoComplete="email"
              placeholder="mariana@exemplo.com"
              maxLength={120}
              autoFocus
            />
          </>
        )}

        {etapa === 4 && (
          <>
            <h2 className="md-titulo">
              Agora, crie uma
              <br />
              senha segura.
            </h2>
            <p className="md-corpo">Use pelo menos 8 caracteres, com letras e números.</p>
            <CampoSenha
              value={valores.senha}
              onChange={(e) => mudar({ senha: e.target.value })}
              erro={erros.senha}
              autoComplete="new-password"
              maxLength={64}
              autoFocus
            />
          </>
        )}

        <Mensagem>{mensagem}</Mensagem>
        <div className="md-espaco-botao" />

        <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
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
