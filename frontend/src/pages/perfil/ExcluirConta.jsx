import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import CampoSenha from '../../components/CampoSenha.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { obrigatorio } from '../../utils/validacao.js';
import './perfil.css';

/**
 * Excluir minha conta (sem tela no Figma; segue o estilo da tela 20).
 * POST /usuarios/me/exclusao com a senha → 204 → encerra a sessão e volta para o início.
 */
export default function ExcluirConta() {
  const { sair } = useSessao();
  const navegar = useNavigate();
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function excluir(e) {
    e.preventDefault();
    setMensagem('');
    const encontrado = obrigatorio(senha, 'Digite sua senha para confirmar.');
    setErro(encontrado);
    if (encontrado) return;

    setEnviando(true);
    try {
      await api('/usuarios/me/exclusao', { metodo: 'POST', corpo: { senha } });
      sair();
      navegar('/', { replace: true });
    } catch (falha) {
      const campo = falha.porCampo().senha;
      setErro(campo ?? '');
      if (!campo) setMensagem(falha.message);
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Excluir minha conta">
      <Cabecalho titulo="Excluir minha conta" voltarPara="/perfil" />
      <h2 className="md-titulo">Tem certeza?</h2>
      <p className="md-corpo">A exclusão não tem volta. Serão apagados:</p>
      <ul className="md-corpo md-lista-exclusao">
        <li>sua conta e as respostas do questionário de Meus Direitos;</li>
        <li>suas denúncias e, junto com elas, os votos, comentários e confirmações que estavam nelas;</li>
        <li>os votos, comentários e confirmações que você fez em denúncias de outras pessoas.</li>
      </ul>

      <form className="d-flex flex-column gap-3" onSubmit={excluir} noValidate>
        <CampoSenha
          rotulo="Digite sua senha para confirmar"
          value={senha}
          onChange={(e) => {
            setSenha(e.target.value);
            setErro('');
          }}
          erro={erro}
          autoComplete="current-password"
          maxLength={64}
        />

        <Mensagem>{mensagem}</Mensagem>

        <button type="submit" className="md-botao md-botao-perigo" disabled={enviando}>
          {enviando ? 'Excluindo…' : 'Excluir minha conta'}
        </button>
        <Link to="/perfil" className="md-botao md-botao-secundario">
          Cancelar
        </Link>
      </form>
    </Tela>
  );
}
