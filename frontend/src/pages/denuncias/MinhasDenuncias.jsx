import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import CardDenuncia from '../../components/CardDenuncia.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import ConfirmarExclusao from './ConfirmarExclusao.jsx';
import './denuncias.css';

function mensagemDeErro(erro) {
  return erro.status === 503 ? 'Serviço indisponível, tente novamente.' : erro.message;
}

/**
 * 17 · Minha denúncia: lista do usuário (GET /denuncias/minhas) com as ações do autor —
 * Editar, Excluir (abre a 18 · Excluir denúncia) e Marcar como resolvida / Reabrir (docs/api.md, 8.7).
 */
export default function MinhasDenuncias() {
  const local = useLocation();
  const [denuncias, setDenuncias] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState(local.state?.aviso ?? '');
  const [ocupada, setOcupada] = useState(null); // id da denúncia com ação em andamento
  const [errosAcao, setErrosAcao] = useState({}); // { [id]: 'mensagem' }
  const [excluir, setExcluir] = useState(null); // denúncia na tela 18
  const [erroExclusao, setErroExclusao] = useState('');

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

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [excluir]);

  async function mudarStatus(denuncia) {
    const resolvida = denuncia.status.codigo === 'RESOLVIDA';
    setOcupada(denuncia.id);
    setErrosAcao((e) => ({ ...e, [denuncia.id]: '' }));
    setAviso('');
    try {
      await api(`/denuncias/${denuncia.id}/status`, {
        metodo: 'PATCH',
        corpo: { status: resolvida ? 'ABERTA' : 'RESOLVIDA' },
      });
      setAviso(resolvida ? 'Denúncia reaberta.' : 'Denúncia marcada como resolvida.');
      await carregar();
    } catch (e) {
      setErrosAcao((atual) => ({ ...atual, [denuncia.id]: mensagemDeErro(e) }));
    } finally {
      setOcupada(null);
    }
  }

  async function confirmarExclusao() {
    setOcupada(excluir.id);
    setErroExclusao('');
    try {
      await api(`/denuncias/${excluir.id}`, { metodo: 'DELETE' });
      setExcluir(null);
      setAviso('Denúncia excluída.');
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
        denuncia={excluir}
        excluindo={ocupada === excluir.id}
        erro={erroExclusao}
        aoConfirmar={confirmarExclusao}
        aoCancelar={() => {
          setExcluir(null);
          setErroExclusao('');
        }}
      />
    );
  }

  return (
    <Tela titulo="Minhas denúncias" comNavegacao>
      <Cabecalho titulo="Minhas denúncias" />
      <Mensagem tipo="info">{aviso}</Mensagem>

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

      {!carregando &&
        !erro &&
        denuncias.map((denuncia) => (
          <section key={denuncia.id} className="d-flex flex-column gap-3" aria-label={`Gerenciar: ${denuncia.titulo}`}>
            <CardDenuncia key={`${denuncia.id}-${denuncia.atualizadaEm}`} denuncia={denuncia} />
            <Resolucao
              denuncia={denuncia}
              ocupada={ocupada === denuncia.id}
              erro={errosAcao[denuncia.id]}
              aoMudarStatus={() => mudarStatus(denuncia)}
            />
            <div className="d-flex gap-2" role="group" aria-label="Gerenciar denúncia">
              <Link to={`/denuncias/${denuncia.id}/editar`} className="md-acao md-acao-gerenciar">
                Editar
              </Link>
              <button
                type="button"
                className="md-acao md-acao-gerenciar perigo"
                onClick={() => setExcluir(denuncia)}
                disabled={ocupada === denuncia.id}
              >
                Excluir
              </button>
            </div>
          </section>
        ))}
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
      <button type="button" className="md-botao md-botao-secundario" onClick={aoMudarStatus} disabled={ocupada}>
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
