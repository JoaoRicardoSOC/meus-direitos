import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf, iniciais } from '../../utils/formatos.js';
import iconeSeta from '../../assets/icones/arrow.svg';
import './perfil.css';

/**
 * 19 · Perfil. Mostra na hora os dados guardados na sessão e atualiza com GET /usuarios/me,
 * guardando a resposta na sessão (entrar) para o resto do app ver nome e cidade novos.
 */
export default function Perfil() {
  const { usuario, entrar, sair } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();
  const aviso = local.state?.aviso ?? '';
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativo = true;
    api('/usuarios/me')
      .then((dados) => {
        if (ativo) entrar(dados);
      })
      .catch((e) => {
        if (ativo) setErro(e.message);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [entrar]);

  function sairDaConta() {
    sair();
    navegar('/', { replace: true });
  }

  if (!usuario) return null; // sessão encerrada: a rota protegida leva para "Entrar"

  return (
    <Tela titulo="Meu perfil" comNavegacao>
      <p className="md-sobretitulo">Minha conta</p>
      <h1 className="md-titulo">Meu perfil</h1>
      <p className="md-corpo">Seus dados, do seu jeito.</p>

      <Mensagem tipo="info">{aviso}</Mensagem>
      {carregando && <Carregando texto="Atualizando seus dados…" />}
      <Mensagem>{erro && `Não foi possível atualizar seus dados agora. ${erro}`}</Mensagem>

      <span className="md-avatar md-avatar-perfil" aria-hidden="true">
        {iniciais(usuario.nome)}
      </span>
      <h2 className="md-subtitulo">{usuario.nome}</h2>
      {/* O Figma diz "MORADORA"; a API não sabe o gênero, então usamos a forma neutra */}
      <span className="md-selo">Morador(a) · {usuario.cidade}</span>

      <section className="md-card" aria-label="Dados da conta">
        <dl className="md-dados-conta">
          <dt>Nome</dt>
          <dd>{usuario.nome}</dd>
          <dt>E-mail</dt>
          <dd>{usuario.email}</dd>
          <dt>Cidade</dt>
          <dd>{cidadeUf(usuario.cidade, usuario.uf)}</dd>
        </dl>
      </section>

      <Link to="/perfil/editar" className="md-botao md-botao-primario">
        Editar meus dados
      </Link>
      <button type="button" className="md-botao md-botao-secundario" onClick={sairDaConta}>
        Sair da conta
      </button>

      <nav aria-label="Mais opções da conta">
        <ul className="md-card md-opcoes-conta">
          <OpcaoConta para="/minhas" rotulo="Minhas denúncias" />
          <OpcaoConta para="/perfil/senha" rotulo="Trocar senha" />
          <OpcaoConta para="/perfil/excluir" rotulo="Excluir minha conta" perigo />
        </ul>
      </nav>
    </Tela>
  );
}

function OpcaoConta({ para, rotulo, perigo = false }) {
  return (
    <li>
      <Link to={para} className={`md-opcao-conta ${perigo ? 'md-opcao-conta-perigo' : ''}`}>
        {rotulo}
        <img src={iconeSeta} alt="" width="20" height="20" />
      </Link>
    </li>
  );
}
