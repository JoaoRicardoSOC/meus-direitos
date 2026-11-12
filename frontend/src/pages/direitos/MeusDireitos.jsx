import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import PrimeiroAcesso from './PrimeiroAcesso.jsx';
import { iconeDoTema } from './iconesDireito.js';
import iconeSetaBaixo from '../../assets/icones/seta-baixo.svg';
import './direitos.css';

/**
 * 28 · Meus Direitos / Possibilidades (GET /usuarios/perfil).
 * 404 = a pessoa ainda não respondeu o questionário → 22 · Primeiro acesso.
 * 200 = cards dos temas que vêm em `direitos` e as tags do perfil.
 */
export default function MeusDireitos() {
  const { usuario } = useSessao();
  const [estado, setEstado] = useState({ carregando: true, erro: '', semPerfil: false, perfil: null });

  const carregar = useCallback(async () => {
    setEstado({ carregando: true, erro: '', semPerfil: false, perfil: null });
    try {
      const perfil = await api('/usuarios/perfil');
      setEstado({ carregando: false, erro: '', semPerfil: false, perfil });
    } catch (e) {
      if (e.status === 404) setEstado({ carregando: false, erro: '', semPerfil: true, perfil: null });
      else setEstado({ carregando: false, erro: e.message, semPerfil: false, perfil: null });
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  if (estado.semPerfil) return <PrimeiroAcesso />;

  const primeiroNome = (usuario?.nome ?? '').trim().split(/\s+/)[0];
  const direitos = estado.perfil?.direitos ?? [];
  const tags = estado.perfil?.tags ?? [];

  return (
    <Tela titulo="Meus Direitos" comNavegacao gap={14}>
      <p className="md-sobretitulo">Informação e inclusão</p>
      <h1 className="md-titulo">Meus Direitos</h1>
      <p className="md-corpo">
        {primeiroNome ? `${primeiroNome}, estes temas podem interessar a você.` : 'Estes temas podem interessar a você.'}
      </p>

      {estado.carregando && <Carregando texto="Carregando seus temas…" />}

      {estado.erro && (
        <>
          <Mensagem>{estado.erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={carregar}>
            Tentar novamente
          </button>
        </>
      )}

      {estado.perfil && (
        <>
          {tags.length > 0 && (
            <ul className="d-flex flex-wrap gap-2 list-unstyled m-0" aria-label="Seu perfil">
              {tags.map((tag) => (
                <li key={tag.codigo} className="md-selo">
                  {tag.descricao}
                </li>
              ))}
            </ul>
          )}

          {/* "Lista rolável de direitos" do Figma: cards, botão e legenda com 12 px entre si */}
          <div className="md-direito-lista">
            {direitos.length > 0 ? (
              <ul className="md-direito-lista list-unstyled m-0">
                {direitos.map((direito) => (
                  <li key={direito.id}>
                    <CardTema direito={direito} />
                  </li>
                ))}
              </ul>
            ) : (
              <>
                <Mensagem tipo="info">
                  Ainda não encontramos temas para o seu perfil. Isso não quer dizer que você não tenha direitos:
                  consulte os canais oficiais ou revise suas respostas.
                </Mensagem>
                {/* Sem temas, não há detalhe que leve à tela 32: o atalho só aparece neste caso */}
                <Link to="/direitos/canais" className="md-botao md-botao-secundario">
                  Consultar canais oficiais
                </Link>
              </>
            )}

            <Link to="/direitos/questionario" className="md-botao md-botao-secundario">
              Revisar meu perfil
            </Link>
            <p className="md-legenda">Esta lista orienta a pesquisa. Não confirma elegibilidade.</p>
          </div>
        </>
      )}
    </Tela>
  );
}

/** Card de um tema: ícone, título, resumo e "Entender os critérios" (abre o detalhe, telas 29–31). */
function CardTema({ direito }) {
  const icone = iconeDoTema(direito.icone);
  return (
    <Link to={`/direitos/${direito.id}`} className="md-card md-direito-card">
      <span className="md-direito-card-titulo">
        <span className="md-direito-icone" aria-hidden="true">
          <img src={icone.src} alt="" width={icone.tamanho} height={icone.tamanho} />
        </span>
        <span className="md-subtitulo">{direito.titulo}</span>
      </span>
      {direito.resumo && <span className="md-corpo">{direito.resumo}</span>}
      <span className="d-flex gap-2 align-items-start justify-content-between md-negrito md-direito-destaque">
        Entender os critérios
        <img src={iconeSetaBaixo} alt="" width="24" height="24" />
      </span>
    </Link>
  );
}
