import { useEffect, useId, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import CardDenuncia from '../../components/CardDenuncia.jsx';
import Mensagem, { Carregando, Toast } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { comportamentoDeRolagem } from '../../utils/foco.js';
import { cidadeUf } from '../../utils/formatos.js';
import ConversaBairro from './ConversaBairro.jsx';
import ComentarioEnviado from './ComentarioEnviado.jsx';
import ResolucaoComunidade from './ResolucaoComunidade.jsx';
import './cidade.css';

/**
 * 10 · Denúncia / Comentários e 11 · Comentário enviado.
 * GET /denuncias/{id} + GET /denuncias/{id}/comentarios. O link "#conversa" do card
 * (que o HashRouter não rola sozinho) leva até o card da conversa.
 */
export default function DetalheDenuncia() {
  const { id } = useParams();
  const local = useLocation();
  const [denuncia, setDenuncia] = useState(null);
  const [comentarios, setComentarios] = useState([]);
  const [erroComentarios, setErroComentarios] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [tentativa, setTentativa] = useState(0);
  const [enviado, setEnviado] = useState(null); // comentário recém-enviado → tela 11
  const [pedidoRolagem, setPedidoRolagem] = useState(0);
  const conversa = useRef(null);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setErro(null);
    setErroComentarios('');
    setEnviado(null);
    Promise.allSettled([api(`/denuncias/${id}`), api(`/denuncias/${id}/comentarios`)]).then(([d, c]) => {
      if (!ativo) return;
      if (d.status === 'rejected') {
        setDenuncia(null);
        setErro(d.reason);
      } else {
        setDenuncia(d.value);
        if (c.status === 'fulfilled') setComentarios(c.value ?? []);
        else {
          setComentarios([]);
          setErroComentarios(c.reason.message);
        }
      }
      setCarregando(false);
    });
    return () => {
      ativo = false;
    };
  }, [id, tentativa]);

  // "#conversa" no endereço (link do card) ou volta da tela 11: rola até a conversa
  // (só depende de quando a tela fica pronta, para votar ou confirmar não rolar a tela de novo)
  const querConversa = local.hash === '#conversa';
  const pronto = !carregando && !enviado && denuncia !== null;
  useEffect(() => {
    if (!pronto) return;
    if (!querConversa && pedidoRolagem === 0) return;
    conversa.current?.scrollIntoView({ behavior: comportamentoDeRolagem(), block: 'start' });
  }, [pronto, querConversa, local.key, pedidoRolagem]);

  function aoEnviarComentario(novo) {
    setComentarios((atuais) => [...atuais, novo]);
    setDenuncia((d) => ({ ...d, comentarios: (d.comentarios ?? 0) + 1 }));
    setEnviado(novo);
  }

  function aoEditarComentario(atualizado) {
    setComentarios((atuais) => atuais.map((c) => (c.id === atualizado.id ? atualizado : c)));
  }

  function aoExcluirComentario(idComentario) {
    setComentarios((atuais) => atuais.filter((c) => c.id !== idComentario));
    setDenuncia((d) => ({ ...d, comentarios: Math.max(0, (d.comentarios ?? 1) - 1) }));
  }

  function voltarDaConfirmacao() {
    setEnviado(null);
    setPedidoRolagem((n) => n + 1);
  }

  // sem histórico (link aberto direto), a seta volta para a cidade
  const voltarPara = local.key === 'default' ? '/cidade' : undefined;

  if (enviado) {
    return (
      <Tela titulo="Comentário enviado" comNavegacao gap={20}>
        <ComentarioEnviado comentario={enviado} aoVoltar={voltarDaConfirmacao} />
      </Tela>
    );
  }

  return (
    <Tela titulo="Detalhes da denúncia" comNavegacao gap={14}>
      <Cabecalho titulo="Detalhes da denúncia" voltarPara={voltarPara} />

      {carregando && <Carregando texto="Carregando denúncia…" />}

      {!carregando && erro?.status === 404 && (
        <div className="md-card">
          <h2 className="md-subtitulo">Denúncia não encontrada</h2>
          <p className="md-corpo">Ela pode ter sido excluída por quem registrou ou o endereço está incorreto.</p>
          <Link to="/cidade" className="md-botao md-botao-primario">
            Voltar para a cidade
          </Link>
        </div>
      )}

      {!carregando && erro && erro.status !== 404 && (
        <div className="d-flex flex-column gap-2">
          <Mensagem>{erro.message}</Mensagem>
          <button type="button" className="md-botao md-botao-secundario" onClick={() => setTentativa((t) => t + 1)}>
            Tentar novamente
          </button>
        </div>
      )}

      {!carregando && denuncia && (
        <div className="d-flex flex-column gap-3">
          <CardDenuncia
            denuncia={denuncia}
            completo
            aoMudarPlacar={(placar) => setDenuncia((d) => ({ ...d, placar }))}
          />
          <Compartilhar denuncia={denuncia} />
          {/* logo depois do card: é a ação que só este app tem; a conversa vem depois */}
          <ResolucaoComunidade denuncia={denuncia} aoAtualizar={setDenuncia} />
          <ConversaBairro
            ref={conversa}
            idDenuncia={denuncia.id}
            comentarios={comentarios}
            erroLista={erroComentarios}
            aoEnviar={aoEnviarComentario}
            aoEditar={aoEditarComentario}
            aoExcluir={aoExcluirComentario}
          />
        </div>
      )}
    </Tela>
  );
}

/**
 * "Compartilhar": o menu do sistema (navigator.share) quando existe; senão copia o link e avisa
 * "Link copiado."; sem nenhum dos dois, mostra o link num campo somente leitura, já selecionado.
 * O link é o endereço atual (com o HashRouter, ele abre esta mesma denúncia; pede login).
 */
function Compartilhar({ denuncia }) {
  const idCampo = useId();
  const campo = useRef(null);
  const [linkVisivel, setLinkVisivel] = useState('');
  const [aviso, setAviso] = useState({ texto: '', chave: 0 });

  useEffect(() => {
    if (linkVisivel) {
      campo.current?.focus();
      campo.current?.select();
    }
  }, [linkVisivel]);

  async function compartilhar() {
    const url = window.location.href;
    if (typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: denuncia.titulo,
          text: `${denuncia.titulo} · ${cidadeUf(denuncia.cidade, denuncia.uf)} · Meus Direitos`,
          url,
        });
        return;
      } catch (erro) {
        if (erro?.name === 'AbortError') return; // a pessoa fechou o menu: nada a fazer
        // outro erro (ex.: sem permissão neste contexto): tenta copiar
      }
    }
    if (navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(url);
        setAviso({ texto: 'Link copiado.', chave: Date.now() });
        return;
      } catch {
        // área de transferência bloqueada (ex.: página aberta por file://): mostra o link
      }
    }
    setLinkVisivel(url);
  }

  return (
    <div className="d-flex flex-column gap-2">
      <button type="button" className="md-acao-texto align-self-start" onClick={compartilhar}>
        Compartilhar
      </button>
      {linkVisivel && (
        <div className="md-campo">
          <label htmlFor={idCampo} className="md-campo-rotulo">
            Link da denúncia
          </label>
          <input
            ref={campo}
            id={idCampo}
            className="form-control md-entrada"
            value={linkVisivel}
            readOnly
            onFocus={(e) => e.target.select()}
            aria-describedby={`${idCampo}-ajuda`}
          />
          <p id={`${idCampo}-ajuda`} className="md-legenda">
            Copie o link e envie para quem quiser.
          </p>
        </div>
      )}
      <Toast texto={aviso.texto} chave={aviso.chave} aoFechar={() => setAviso((a) => ({ ...a, texto: '' }))} />
    </div>
  );
}
