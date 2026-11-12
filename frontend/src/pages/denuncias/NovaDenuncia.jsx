import { useEffect, useId, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Campo from '../../components/Campo.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { obrigatorio, tamanho, validarCampos } from '../../utils/validacao.js';
import { focarPrimeiroErro } from '../../utils/foco.js';
import EscolherCategoria from './EscolherCategoria.jsx';
import EscolherFoto from './EscolherFoto.jsx';
import DenunciaPublicada from './DenunciaPublicada.jsx';
import { CATEGORIAS_PADRAO, EXEMPLO, descricaoDaFoto } from './opcoes.js';
import iconeChevron from '../../assets/icones/seta-campo.svg';
import iconePin from '../../assets/icones/pin-campo.svg';
import iconeCamera from '../../assets/icones/camera.svg';
import './denuncias.css';

const VAZIO = { titulo: '', descricao: '', categoria: '', endereco: '', bairro: '', imagemUrl: '' };

/** Mesmas regras do back-end (docs/api.md, 8.5). */
const REGRAS = {
  titulo: (v) => tamanho(v, 5, 120),
  descricao: (v) => tamanho(v, 10, 1000),
  categoria: (v) => obrigatorio(v, 'Escolha uma categoria.'),
  endereco: (v) => tamanho(v, 5, 150),
  bairro: (v) => tamanho(v, 0, 80),
};

function corpoDaRequisicao(valores) {
  const corpo = {
    titulo: valores.titulo.trim(),
    descricao: valores.descricao.trim(),
    categoria: valores.categoria,
    endereco: valores.endereco.trim(),
  };
  if (valores.bairro.trim()) corpo.bairro = valores.bairro.trim();
  if (valores.imagemUrl) corpo.imagemUrl = valores.imagemUrl;
  return corpo;
}

/** Rascunho da denúncia nova: fica na aba até publicar (a edição não usa rascunho). */
const CHAVE_RASCUNHO = 'md-rascunho-denuncia';

function lerRascunho() {
  try {
    const salvo = JSON.parse(sessionStorage.getItem(CHAVE_RASCUNHO) ?? 'null');
    if (!salvo || typeof salvo !== 'object') return VAZIO;
    const valores = { ...VAZIO };
    Object.keys(VAZIO).forEach((campo) => {
      if (typeof salvo[campo] === 'string') valores[campo] = salvo[campo];
    });
    return valores;
  } catch {
    return VAZIO;
  }
}

function salvarRascunho(valores) {
  try {
    const vazio = Object.values(valores).every((valor) => !String(valor).trim());
    if (vazio) sessionStorage.removeItem(CHAVE_RASCUNHO);
    else sessionStorage.setItem(CHAVE_RASCUNHO, JSON.stringify(valores));
  } catch {
    // sem armazenamento (aba privada, cota cheia): o formulário segue funcionando sem rascunho
  }
}

function apagarRascunho() {
  try {
    sessionStorage.removeItem(CHAVE_RASCUNHO);
  } catch {
    // idem
  }
}

const ETAPAS_DO_FORMULARIO = ['categoria', 'foto'];

/**
 * 12 · Nova denúncia / Inicial, 13 · Preenchida, 14 · Categorias, 15 · Foto / Prévia e 16 · Denúncia publicada.
 * O mesmo componente cria (/denuncias/nova → POST) e edita (/denuncias/:id/editar → PUT).
 * As telas 14, 15 e 16 são etapas na URL (?passo=categoria|foto|publicada): o "voltar" do celular
 * volta ao formulário sem perder o que foi digitado, na mesma rolagem e no botão que abriu a etapa.
 */
export default function NovaDenuncia() {
  const { id } = useParams();
  const editando = Boolean(id);
  const { usuario } = useSessao();
  const usuarioId = usuario?.id;
  const navegar = useNavigate();
  const local = useLocation();
  const [parametros] = useSearchParams();
  const idForm = useId();
  const formRef = useRef(null);
  const botaoCategoriaRef = useRef(null);
  const botaoFotoRef = useRef(null);
  const retorno = useRef(null); // { rolagem, botao }: guardado ao abrir uma etapa a partir do formulário

  const [valores, setValores] = useState(() => (editando ? VAZIO : lerRascunho()));
  const [categorias, setCategorias] = useState(CATEGORIAS_PADRAO);
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [carregando, setCarregando] = useState(editando);
  const [erroCarga, setErroCarga] = useState('');

  // Etapa atual, lida da URL. "publicada" só existe com a resposta do POST no estado do histórico.
  const passoNaUrl = parametros.get('passo');
  const publicada = local.state?.publicada;
  let passo = 'formulario';
  if (ETAPAS_DO_FORMULARIO.includes(passoNaUrl)) passo = passoNaUrl;
  else if (passoNaUrl === 'publicada' && publicada && !editando) passo = 'publicada';

  // Direção da entrada: as etapas avançam pela direita; a volta ao formulário vem pela esquerda
  const [passoAnterior, setPassoAnterior] = useState(passo);
  const [direcao, setDirecao] = useState(undefined);
  if (passoAnterior !== passo) {
    setPassoAnterior(passo);
    setDirecao(passo === 'formulario' ? 'tras' : undefined);
  }

  // ?passo= desconhecido (ou "publicada" sem a resposta): volta ao formulário, no lugar
  useEffect(() => {
    if (passoNaUrl && passo === 'formulario') navegar({ search: '' }, { replace: true });
  }, [passoNaUrl, passo, navegar]);

  // Opções da tela 14. Se a API falhar, ficam as categorias documentadas (o envio mostrará o erro).
  useEffect(() => {
    let ativo = true;
    api('/categorias')
      .then((lista) => ativo && Array.isArray(lista) && lista.length > 0 && setCategorias(lista))
      .catch(() => {});
    return () => {
      ativo = false;
    };
  }, []);

  // Edição: carrega a denúncia e pré-preenche o formulário
  useEffect(() => {
    if (!editando) return undefined;
    let ativo = true;
    setCarregando(true);
    setErroCarga('');
    api(`/denuncias/${id}`)
      .then((denuncia) => {
        if (!ativo) return;
        if (usuarioId && denuncia.autor.id !== usuarioId) {
          setErroCarga('Só quem registrou a denúncia pode editá-la.');
          return;
        }
        setValores({
          titulo: denuncia.titulo ?? '',
          descricao: denuncia.descricao ?? '',
          categoria: denuncia.categoria?.codigo ?? '',
          endereco: denuncia.endereco ?? '',
          bairro: denuncia.bairro ?? '',
          imagemUrl: denuncia.imagemUrl ?? '',
        });
      })
      .catch((erro) => ativo && setErroCarga(erro.status === 404 ? 'Denúncia não encontrada.' : erro.message))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [editando, id, usuarioId]);

  // Rascunho da denúncia nova
  useEffect(() => {
    if (!editando) salvarRascunho(valores);
  }, [editando, valores]);

  // De volta ao formulário: a mesma rolagem de antes (na hora) e o foco no botão que abriu a etapa.
  // Dois quadros: no primeiro o GerenciarRota foca o título; no segundo o foco vem para o botão.
  useEffect(() => {
    if (passo !== 'formulario' || !retorno.current) return undefined;
    const { rolagem, botao } = retorno.current;
    retorno.current = null;
    let quadro = requestAnimationFrame(() => {
      quadro = requestAnimationFrame(() => {
        window.scrollTo({ top: rolagem, left: 0, behavior: 'instant' });
        botao.current?.focus({ preventScroll: true });
      });
    });
    return () => cancelAnimationFrame(quadro);
  }, [passo]);

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
  }

  /** Abre a etapa empilhando no histórico (o GerenciarRota leva ao topo e foca o título). */
  function abrir(etapa, botao) {
    retorno.current = { rolagem: window.scrollY, botao };
    navegar({ search: `?passo=${etapa}` }, { state: { doFormulario: true } });
  }

  /** Volta ao formulário: desempilha a etapa; se ela foi aberta direto pela URL, troca no lugar. */
  function voltarAoFormulario() {
    if (local.state?.doFormulario) navegar(-1);
    else navegar({ search: '' }, { replace: true });
  }

  function preencherExemplo() {
    setValores(EXEMPLO);
    setErros({});
    setMensagem('');
  }

  async function enviar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = validarCampos(valores, REGRAS);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) {
      setMensagem('Preencha os campos destacados para publicar.');
      focarPrimeiroErro(formRef.current);
      return;
    }

    setEnviando(true);
    try {
      const corpo = corpoDaRequisicao(valores);
      if (editando) {
        await api(`/denuncias/${id}`, { metodo: 'PUT', corpo });
        navegar('/minhas', { state: { aviso: 'Denúncia atualizada.' } });
      } else {
        const resposta = await api('/denuncias', { metodo: 'POST', corpo });
        apagarRascunho();
        setValores(VAZIO);
        // Troca no lugar: o "voltar" da confirmação não reabre o formulário já enviado
        navegar({ search: '?passo=publicada' }, { replace: true, state: { publicada: resposta } });
      }
    } catch (erro) {
      if (erro.status === 409) {
        setErros({ titulo: erro.message });
        setMensagem('Verifique o campo destacado.');
      } else {
        setErros(erro.porCampo());
        setMensagem(erro.status === 503 ? 'Serviço indisponível, tente novamente.' : erro.message);
      }
      focarPrimeiroErro(formRef.current);
    } finally {
      setEnviando(false);
    }
  }

  const tituloTela = editando ? 'Editar denúncia' : 'Nova denúncia';

  if (carregando || erroCarga) {
    return (
      <Tela titulo={tituloTela}>
        <Cabecalho titulo={tituloTela} />
        {carregando ? <Carregando texto="Carregando a denúncia…" /> : <Mensagem>{erroCarga}</Mensagem>}
      </Tela>
    );
  }

  if (passo === 'publicada') {
    return <DenunciaPublicada denuncia={publicada} />;
  }

  if (passo === 'categoria') {
    return (
      <EscolherCategoria
        categorias={categorias}
        selecionada={valores.categoria}
        aoEscolher={(codigo) => {
          // trocar de categoria tira uma foto que era de outra categoria
          const fotoDeOutra = valores.imagemUrl && valores.categoria && valores.categoria !== codigo;
          mudar(fotoDeOutra ? { categoria: codigo, imagemUrl: '' } : { categoria: codigo });
          voltarAoFormulario();
        }}
        aoVoltar={voltarAoFormulario}
      />
    );
  }

  if (passo === 'foto') {
    return (
      <EscolherFoto
        categoria={valores.categoria}
        atual={valores.imagemUrl}
        aoUsar={(caminho) => {
          mudar({ imagemUrl: caminho });
          voltarAoFormulario();
        }}
        aoVoltar={voltarAoFormulario}
      />
    );
  }

  const nomeCategoria = categorias.find((c) => c.codigo === valores.categoria)?.descricao;

  return (
    <Tela titulo={tituloTela}>
      <div key="formulario" className="md-etapa" data-direcao={direcao}>
        <Cabecalho titulo={tituloTela} />

        <div className="d-flex flex-column gap-2">
          <h2 className="md-titulo">O que precisa de atenção?</h2>
          <p className="md-corpo">Conte o que está acontecendo no seu bairro.</p>
          {!editando && (
            <button type="button" className="md-acao-texto align-self-start" onClick={preencherExemplo}>
              Usar um exemplo
            </button>
          )}
        </div>

        <form
          ref={formRef}
          className="d-flex flex-column md-grupo"
          style={{ gap: 20 }}
          onSubmit={enviar}
          noValidate
        >
          <Campo
            rotulo="Título do problema"
            value={valores.titulo}
            onChange={(e) => mudar({ titulo: e.target.value })}
            erro={erros.titulo}
            placeholder="Ex.: buraco na rua"
            maxLength={120}
          />

          <Campo
            rotulo="Descrição"
            tipo="textarea"
            rows={2}
            value={valores.descricao}
            onChange={(e) => mudar({ descricao: e.target.value })}
            erro={erros.descricao}
            placeholder="Descreva o problema e o impacto."
            maxLength={1000}
          />

          {/* Categoria: abre a tela 14 */}
          <div className="md-campo">
            <span id={`${idForm}-rotulo`} className="md-campo-rotulo">
              Categoria
            </span>
            <button
              ref={botaoCategoriaRef}
              type="button"
              className={`md-entrada-botao ${nomeCategoria ? '' : 'vazio'} ${erros.categoria ? 'is-invalid' : ''}`}
              onClick={() => abrir('categoria', botaoCategoriaRef)}
              aria-labelledby={`${idForm}-rotulo ${idForm}-valor`}
              aria-invalid={erros.categoria ? true : undefined}
              aria-describedby={erros.categoria ? `${idForm}-erro` : undefined}
            >
              <span id={`${idForm}-valor`}>{nomeCategoria ?? 'Selecione uma categoria'}</span>
              <img src={iconeChevron} alt="" width="20" height="20" />
            </button>
            {erros.categoria && (
              <p id={`${idForm}-erro`} className="md-erro-campo">
                {erros.categoria}
              </p>
            )}
          </div>

          {/* O Figma tem só "Endereço"; a API pede endereço e bairro separados (pedido 2).
              Sem preenchimento automático: a denúncia é pública e não deve puxar o endereço de casa. */}
          <div className="md-campo">
            <label htmlFor={`${idForm}-endereco`} className="md-campo-rotulo">
              Endereço
            </label>
            <div className="md-entrada-com-icone">
              <input
                id={`${idForm}-endereco`}
                className={`form-control md-entrada ${erros.endereco ? 'is-invalid' : ''}`}
                value={valores.endereco}
                onChange={(e) => mudar({ endereco: e.target.value })}
                placeholder="Rua e número"
                autoComplete="off"
                maxLength={150}
                aria-invalid={erros.endereco ? true : undefined}
                aria-describedby={erros.endereco ? `${idForm}-erro-endereco` : undefined}
              />
              <span className="md-icone-campo" aria-hidden="true">
                <img src={iconePin} alt="" width="20" height="20" />
              </span>
            </div>
            {erros.endereco && (
              <p id={`${idForm}-erro-endereco`} className="md-erro-campo">
                {erros.endereco}
              </p>
            )}
          </div>

          <Campo
            rotulo="Bairro (opcional)"
            value={valores.bairro}
            onChange={(e) => mudar({ bairro: e.target.value })}
            erro={erros.bairro}
            placeholder="Ex.: Centro"
            maxLength={80}
            autoComplete="off"
          />

          {/* Foto: abre a tela 15 */}
          <div className="d-flex flex-column gap-2">
            <button
              ref={botaoFotoRef}
              type="button"
              className="md-opcao"
              onClick={() => abrir('foto', botaoFotoRef)}
              aria-invalid={erros.imagemUrl ? true : undefined}
            >
              <img src={iconeCamera} alt="" width="24" height="24" />
              {valores.imagemUrl ? 'Trocar a foto' : 'Adicionar foto do problema'}
            </button>
            {valores.imagemUrl && (
              <div className="md-foto-anexada">
                <img
                  src={valores.imagemUrl}
                  alt={`Foto ilustrativa: ${descricaoDaFoto(valores.imagemUrl)}`}
                  className="md-foto-previa"
                />
                <button
                  type="button"
                  className="md-acao-texto"
                  onClick={() => {
                    mudar({ imagemUrl: '' });
                    botaoFotoRef.current?.focus();
                  }}
                >
                  Remover foto
                </button>
              </div>
            )}
            {erros.imagemUrl && <p className="md-erro-campo">{erros.imagemUrl}</p>}
          </div>

          <div className="d-flex flex-column md-grupo" style={{ gap: 12 }}>
            <Mensagem>{mensagem}</Mensagem>
            <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
              {enviando ? 'Enviando…' : editando ? 'Salvar alterações' : 'Publicar denúncia'}
            </button>
            <p className="md-legenda">Não inclua rostos ou dados de outras pessoas.</p>
          </div>
        </form>
      </div>
    </Tela>
  );
}
