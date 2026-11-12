import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import CardDenuncia from '../../components/CardDenuncia.jsx';
import Mensagem, { Carregando, Toast } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { irParaOTopo } from '../../utils/foco.js';
import ConfirmarExclusao from './ConfirmarExclusao.jsx';
import iconeEditar from '../../assets/icones/editar.svg';
import iconeLixeira from '../../assets/icones/lixeira.svg';
import './denuncias.css';

function mensagemDeErro(erro) {
  return erro.status === 503 ? 'Serviço indisponível, tente novamente.' : erro.message;
}

/**
 * 17 · Minha denúncia: lista do usuário (GET /denuncias/minhas) com as ações do autor —
 * Editar, Excluir (abre a 18 · Excluir denúncia) e Marcar como resolvida / Reabrir (docs/api.md, 8.7).
 * O resultado de cada ação aparece num aviso temporário (Toast) perto da navegação.
 */
export default function MinhasDenuncias() {
  const local = useLocation();
  const navegar = useNavigate();
  const [denuncias, setDenuncias] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState(() => ({ texto: local.state?.aviso ?? '', chave: Date.now() }));
  const [ocupada, setOcupada] = useState(null); // id da denúncia com ação em andamento
  const [errosAcao, setErrosAcao] = useState({}); // { [id]: 'mensagem' }
  const [excluir, setExcluir] = useState(null); // denúncia na tela 18
  const [erroExclusao, setErroExclusao] = useState('');
  const retorno = useRef(null); // { rolagem, id }: para voltar ao mesmo lugar ao manter a denúncia

  const avisar = (texto) => setAviso({ texto, chave: Date.now() });

  // O aviso vindo da edição ("Denúncia atualizada.") aparece uma vez só: sai do estado do histórico
  useEffect(() => {
    if (local.state?.aviso) navegar({ pathname: local.pathname, search: local.search }, { replace: true, state: null });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const carregar = useCallback(async () => {
    setErro('');
    try {
      const lista = await api('/denuncias/minhas');
      setDenuncias(Array.isArray(lista) ? lista : []);
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  // Mantida a denúncia: a lista volta na mesma rolagem, com o foco no "Excluir" que abriu a 18
  useEffect(() => {
    if (excluir || !retorno.current) return undefined;
    const { rolagem, id } = retorno.current;
    retorno.current = null;
    const quadro = requestAnimationFrame(() => {
      window.scrollTo({ top: rolagem, left: 0, behavior: 'instant' });
      document.querySelector(`[data-excluir="${id}"]`)?.focus({ preventScroll: true });
    });
    return () => cancelAnimationFrame(quadro);
  }, [excluir]);

  function abrirExclusao(denuncia) {
    retorno.current = { rolagem: window.scrollY, id: denuncia.id };
    setErroExclusao('');
    setExcluir(denuncia);
    // topo na hora; o foco inicial fica em "Manter denúncia" (ConfirmarExclusao)
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  }

  function manterDenuncia() {
    setExcluir(null);
    setErroExclusao('');
  }

  async function mudarStatus(denuncia) {
    const resolvida = denuncia.status.codigo === 'RESOLVIDA';
    setOcupada(denuncia.id);
    setErrosAcao((e) => ({ ...e, [denuncia.id]: '' }));
    try {
      await api(`/denuncias/${denuncia.id}/status`, {
        metodo: 'PATCH',
        corpo: { status: resolvida ? 'ABERTA' : 'RESOLVIDA' },
      });
      avisar(resolvida ? 'Denúncia reaberta.' : 'Denúncia marcada como resolvida.');
      await carregar();
    } catch (e) {
      setErrosAcao((atual) => ({ ...atual, [denuncia.id]: mensagemDeErro(e) }));
    } finally {
      setOcupada(null);
    }
  }

  async function confirmarExclusao() {
    const alvo = excluir;
    setOcupada(alvo.id);
    setErroExclusao('');
    try {
      await api(`/denuncias/${alvo.id}`, { metodo: 'DELETE' });
      retorno.current = null; // a denúncia sumiu: o foco vai para o título, no topo
      setDenuncias((lista) => lista.filter((d) => d.id !== alvo.id));
      setExcluir(null);
      avisar('Denúncia excluída.');
      irParaOTopo();
      await carregar();
    } catch (e) {
      setErroExclusao(mensagemDeErro(e));
    } finally {
      setOcupada(null);
    }
  }

  if (excluir) {
    return (
      <ConfirmarExclusao
        excluindo={ocupada === excluir.id}
        erro={erroExclusao}
        aoConfirmar={confirmarExclusao}
        aoCancelar={manterDenuncia}
      />
    );
  }

  return (
    <Tela titulo="Minhas denúncias" comNavegacao>
      <Cabecalho titulo="Minhas denúncias" />

      {carregando && <Carregando texto="Carregando suas denúncias…" />}

      {!carregando && erro && (
        <>
          <Mensagem>{erro}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={carregar}>
            Tentar novamente
          </button>
        </>
      )}

      {!carregando && !erro && denuncias.length === 0 && (
        <section className="md-card align-items-start" aria-labelledby="titulo-vazio">
          <h2 id="titulo-vazio" className="md-subtitulo">Você ainda não registrou nenhuma denúncia.</h2>
          <p className="md-corpo">Viu algo no bairro que precisa de atenção? Conte para a comunidade.</p>
          <Link to="/denuncias/nova" className="md-botao md-botao-primario">
            Registrar minha primeira denúncia
          </Link>
        </section>
      )}

      {!carregando && !erro && denuncias.length > 0 && (
        <div className="md-lista-gerenciar">
          {denuncias.map((denuncia) => (
            <section key={denuncia.id} className="md-bloco-gerenciar" aria-label={`Gerenciar: ${denuncia.titulo}`}>
              <CardDenuncia key={`${denuncia.id}-${denuncia.atualizadaEm}`} denuncia={denuncia} />
              <div className="md-acoes-gerenciar" role="group" aria-label="Gerenciar denúncia">
                <Link to={`/denuncias/${denuncia.id}/editar`} className="md-acao-texto">
                  <img src={iconeEditar} alt="" width="20" height="20" />
                  Editar
                </Link>
                <button
                  type="button"
                  className="md-acao-texto md-perigo"
                  onClick={() => abrirExclusao(denuncia)}
                  disabled={ocupada === denuncia.id}
                  data-excluir={denuncia.id}
                >
                  <img src={iconeLixeira} alt="" width="20" height="20" />
                  Excluir
                </button>
              </div>
              <Resolucao
                denuncia={denuncia}
                ocupada={ocupada === denuncia.id}
                erro={errosAcao[denuncia.id]}
                aoMudarStatus={() => mudarStatus(denuncia)}
              />
            </section>
          ))}
        </div>
      )}

      <Toast texto={aviso.texto} chave={aviso.chave} aoFechar={() => setAviso((a) => ({ ...a, texto: '' }))} />
    </Tela>
  );
}

/** Progresso "X de 3 moradores confirmaram" e o botão "Marcar como resolvida" / "Reabrir". */
function Resolucao({ denuncia, ocupada, erro, aoMudarStatus }) {
  const resolvida = denuncia.status.codigo === 'RESOLVIDA';
  const { confirmacoes = 0, necessarias = 3 } = denuncia.resolucao ?? {};

  return (
    <div className="d-flex flex-column gap-2">
      {!resolvida && (
        <>
          <div
            className="md-barra-resolucao"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={necessarias}
            aria-valuenow={confirmacoes}
            aria-label="Confirmações de que foi resolvido"
          >
            {Array.from({ length: necessarias }, (_, i) => (
              <span key={i} className={i < confirmacoes ? 'ativa' : ''} />
            ))}
          </div>
          <p className="md-legenda">
            {confirmacoes} de {necessarias} moradores confirmaram que foi resolvido
          </p>
        </>
      )}
      {resolvida && <p className="md-legenda">Esta denúncia está resolvida.</p>}
      <button
        type="button"
        className={`md-botao ${resolvida ? 'md-botao-secundario' : 'md-botao-primario'}`}
        onClick={aoMudarStatus}
        disabled={ocupada}
      >
        {ocupada ? 'Salvando…' : resolvida ? 'Reabrir' : 'Marcar como resolvida'}
      </button>
      {erro && (
        <p className="md-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </div>
  );
}
