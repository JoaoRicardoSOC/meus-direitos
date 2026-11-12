import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Campo from '../../components/Campo.jsx';
import CampoSenha from '../../components/CampoSenha.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { obrigatorio, validar, validarCampos } from '../../utils/validacao.js';
import { focarPrimeiroErro } from '../../utils/foco.js';
import './inicio.css';

/** 06 · Entrar (POST /auth/login) */
export default function Entrar() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();
  const [valores, setValores] = useState({ email: '', senha: '' });
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const formulario = useRef(null);
  const titulo = useRef(null);

  // O título da tela é a pergunta, não o nome do app no cabeçalho: o foco vai para ele
  // (dois quadros: depois do foco que o GerenciarRota põe no título do cabeçalho)
  useEffect(() => {
    const quadro = requestAnimationFrame(() => {
      requestAnimationFrame(() => titulo.current?.focus({ preventScroll: true }));
    });
    return () => cancelAnimationFrame(quadro);
  }, []);

  function mudar(campo) {
    return (e) => {
      setValores((v) => ({ ...v, [campo]: e.target.value }));
      setErros((atual) => ({ ...atual, [campo]: '' }));
    };
  }

  async function enviar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = validarCampos(valores, { email: validar.email, senha: (v) => obrigatorio(v) });
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) {
      setMensagem(Object.keys(encontrados).length > 1 ? 'Revise os campos destacados.' : 'Revise o campo destacado.');
      focarPrimeiroErro(formulario.current);
      return;
    }

    setEnviando(true);
    try {
      const usuario = await api('/auth/login', { metodo: 'POST', corpo: valores });
      entrar(usuario);
      navegar(local.state?.voltarPara ?? '/cidade', { replace: true });
    } catch (erro) {
      setMensagem(erro.message);
      // 429: muitas tentativas. A mensagem vai só no resumo (role="alert"), sem apontar campo,
      // e o que foi digitado continua nos campos para tentar de novo depois.
      if (erro.status === 429) {
        setErros({});
        return;
      }
      setErros(erro.porCampo());
      focarPrimeiroErro(formulario.current);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Entrar">
      <Cabecalho titulo="Meus Direitos" voltarPara="/" />

      <h2 className="md-titulo md-grupo" ref={titulo} tabIndex={-1} data-foco-tela>
        Entre na sua conta
      </h2>
      <p className="md-corpo">Bom ter você de volta. Acompanhe o que acontece na sua cidade.</p>

      <form ref={formulario} className="md-formulario-inicio md-grupo" onSubmit={enviar} noValidate>
        <Mensagem>{mensagem}</Mensagem>
        <Campo
          rotulo="E-mail"
          type="email"
          value={valores.email}
          onChange={mudar('email')}
          erro={erros.email}
          autoComplete="email"
          placeholder="Ex.: maria@email.com"
        />
        <CampoSenha
          value={valores.senha}
          onChange={mudar('senha')}
          erro={erros.senha}
          autoComplete="current-password"
        />
        <button type="submit" className="md-botao md-botao-primario md-grupo" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
        <Link to="/cadastro" className="md-botao md-botao-secundario">
          Criar uma conta
        </Link>
      </form>
    </Tela>
  );
}
