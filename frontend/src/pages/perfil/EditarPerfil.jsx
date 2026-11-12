import { useState } from 'react';
import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Campo from '../../components/Campo.jsx';
import CampoCidade from '../../components/CampoCidade.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { cidadeUf } from '../../utils/formatos.js';
import { validar, validarCampos } from '../../utils/validacao.js';
import './perfil.css';

const REGRAS = {
  nome: validar.nome,
  email: validar.email,
  cidade: validar.cidade,
  uf: validar.uf,
};

/**
 * 20 · Perfil / Editar → 21 · Perfil / Dados salvos.
 * PUT /usuarios/me; a resposta vira a nova sessão (entrar) e a tela mostra a confirmação.
 */
export default function EditarPerfil() {
  const { usuario, entrar } = useSessao();
  const [valores, setValores] = useState(() => ({
    nome: usuario?.nome ?? '',
    email: usuario?.email ?? '',
    cidade: usuario?.cidade ?? '',
    uf: usuario?.uf ?? '',
  }));
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [salvo, setSalvo] = useState(null);

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
  }

  async function salvar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = validarCampos(valores, REGRAS);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) return;

    setEnviando(true);
    try {
      const corpo = {
        nome: valores.nome.trim(),
        email: valores.email.trim(),
        cidade: valores.cidade.trim(),
        uf: valores.uf,
      };
      const atualizado = await api('/usuarios/me', { metodo: 'PUT', corpo });
      entrar(atualizado);
      setSalvo(atualizado);
    } catch (erro) {
      if (erro.status === 409) {
        setErros({ email: erro.message }); // e-mail de outra conta
      } else {
        setErros(erro.porCampo());
        setMensagem(erro.message);
      }
    } finally {
      setEnviando(false);
    }
  }

  if (salvo) return <DadosSalvos usuario={salvo} />;

  return (
    <Tela titulo="Editar meus dados">
      <Cabecalho titulo="Editar meus dados" voltarPara="/perfil" />
      <h2 className="md-titulo">Vamos atualizar?</h2>
      <p className="md-corpo">Mantenha suas informações em dia.</p>

      <form className="d-flex flex-column gap-3" onSubmit={salvar} noValidate>
        <Campo
          rotulo="Nome"
          value={valores.nome}
          onChange={(e) => mudar({ nome: e.target.value })}
          erro={erros.nome}
          autoComplete="name"
          maxLength={100}
        />
        <Campo
          rotulo="E-mail"
          type="email"
          value={valores.email}
          onChange={(e) => mudar({ email: e.target.value })}
          erro={erros.email}
          autoComplete="email"
          maxLength={120}
        />
        <CampoCidade cidade={valores.cidade} uf={valores.uf} aoMudar={mudar} erros={erros} />

        <Mensagem>{mensagem}</Mensagem>

        <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
          {enviando ? 'Salvando…' : 'Salvar alterações'}
        </button>
        <Link to="/perfil" className="md-botao md-botao-secundario">
          Cancelar
        </Link>
      </form>
    </Tela>
  );
}

/** 21 · Perfil / Dados salvos: confirmação depois do PUT, com os dados que ficaram gravados. */
function DadosSalvos({ usuario }) {
  return (
    <Tela titulo="Dados salvos">
      <Cabecalho titulo="Editar meus dados" voltarPara="/perfil" />
      <div role="status" className="d-flex flex-column gap-3">
        <p className="md-sobretitulo">Dados salvos</p>
        <h2 className="md-titulo">Tudo certo!</h2>
        <p className="md-corpo">Suas informações foram atualizadas.</p>
      </div>

      <section className="md-card" aria-label="Dados atualizados">
        <dl className="md-dados-conta">
          <dt>Nome</dt>
          <dd>{usuario.nome}</dd>
          <dt>E-mail</dt>
          <dd>{usuario.email}</dd>
          <dt>Cidade</dt>
          <dd>{cidadeUf(usuario.cidade, usuario.uf)}</dd>
        </dl>
      </section>

      <Link to="/perfil" className="md-botao md-botao-primario">
        Voltar ao perfil
      </Link>
    </Tela>
  );
}
