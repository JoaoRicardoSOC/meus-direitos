import { useEffect, useId, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import Campo from '../../components/Campo.jsx';
import Mensagem, { Carregando } from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { useSessao } from '../../contexts/SessaoContext.jsx';
import { obrigatorio, tamanho, validarCampos } from '../../utils/validacao.js';
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

/**
 * 12 · Nova denúncia / Inicial, 13 · Preenchida, 14 · Categorias, 15 · Foto / Prévia e 16 · Denúncia publicada.
 * O mesmo componente cria (/denuncias/nova → POST) e edita (/denuncias/:id/editar → PUT).
 * As telas 14 e 15 são passos internos: a escolha volta para o formulário sem perder o que foi digitado.
 */
export default function NovaDenuncia() {
  const { id } = useParams();
  const editando = Boolean(id);
  const { usuario } = useSessao();
  const usuarioId = usuario?.id;
  const navegar = useNavigate();
  const idForm = useId();

  const [passo, setPasso] = useState('formulario'); // formulario | categorias | foto | publicada
  const [valores, setValores] = useState(VAZIO);
  const [categorias, setCategorias] = useState(CATEGORIAS_PADRAO);
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [carregando, setCarregando] = useState(editando);
  const [erroCarga, setErroCarga] = useState('');
  const [publicada, setPublicada] = useState(null);

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

  // Cada passo começa no topo da página
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [passo]);

  function mudar(parcial) {
    setValores((v) => ({ ...v, ...parcial }));
    setErros((atual) => {
      const copia = { ...atual };
      Object.keys(parcial).forEach((campo) => delete copia[campo]);
      return copia;
    });
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
      setMensagem('Verifique os campos destacados.');
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
        setPublicada(resposta);
        setPasso('publicada');
      }
    } catch (erro) {
      if (erro.status === 409) {
        setErros({ titulo: erro.message });
      } else {
        setErros(erro.porCampo());
        setMensagem(erro.status === 503 ? 'Serviço indisponível, tente novamente.' : erro.message);
      }
    } finally {
      setEnviando(false);
    }
  }

  const tituloTela = editando ? 'Editar denúncia' : 'Nova denúncia';

  if (passo === 'publicada' && publicada) {
    return <DenunciaPublicada denuncia={publicada} />;
  }

  if (passo === 'categorias') {
    return (
      <EscolherCategoria
        categorias={categorias}
        selecionada={valores.categoria}
        aoEscolher={(codigo) => {
          // trocar de categoria tira uma foto que era de outra categoria
          const fotoDeOutra = valores.imagemUrl && valores.categoria && valores.categoria !== codigo;
          mudar(fotoDeOutra ? { categoria: codigo, imagemUrl: '' } : { categoria: codigo });
          setPasso('formulario');
        }}
        aoVoltar={() => setPasso('formulario')}
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
          setPasso('formulario');
        }}
        aoVoltar={() => setPasso('formulario')}
      />
    );
  }

  if (carregando || erroCarga) {
    return (
      <Tela titulo={tituloTela}>
        <Cabecalho titulo={tituloTela} />
        {carregando ? <Carregando texto="Carregando a denúncia…" /> : <Mensagem>{erroCarga}</Mensagem>}
      </Tela>
    );
  }

  const nomeCategoria = categorias.find((c) => c.codigo === valores.categoria)?.descricao;
  const vazio = Object.values(valores).every((valor) => !String(valor).trim());

  return (
    <Tela titulo={tituloTela} gap={12}>
      <Cabecalho titulo={tituloTela} />
      <h2 className="md-titulo">O que precisa de atenção?</h2>
      <p className="md-corpo">Conte o que está acontecendo no seu bairro.</p>

      <form className="d-flex flex-column" style={{ gap: 12 }} onSubmit={enviar} noValidate>
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
            type="button"
            className={`md-entrada-botao ${nomeCategoria ? '' : 'vazio'} ${erros.categoria ? 'is-invalid' : ''}`}
            onClick={() => setPasso('categorias')}
            aria-labelledby={`${idForm}-rotulo ${idForm}-valor`}
            aria-describedby={erros.categoria ? `${idForm}-erro` : undefined}
          >
            <span id={`${idForm}-valor`}>{nomeCategoria ?? 'Selecione uma categoria'}</span>
            <img src={iconeChevron} alt="" width="20" height="20" />
          </button>
          {erros.categoria && (
            <p id={`${idForm}-erro`} className="md-erro-campo" role="alert">
              {erros.categoria}
            </p>
          )}
        </div>

        {/* O Figma tem só "Endereço"; a API pede endereço e bairro separados (pedido 2) */}
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
              autoComplete="street-address"
              maxLength={150}
              aria-invalid={erros.endereco ? true : undefined}
              aria-describedby={erros.endereco ? `${idForm}-erro-endereco` : undefined}
            />
            <span className="md-icone-campo" aria-hidden="true">
              <img src={iconePin} alt="" width="20" height="20" />
            </span>
          </div>
          {erros.endereco && (
            <p id={`${idForm}-erro-endereco`} className="md-erro-campo" role="alert">
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
        />

        {/* Foto: abre a tela 15 */}
        <div className="d-flex flex-column gap-2">
          <button type="button" className="md-opcao" onClick={() => setPasso('foto')}>
            <img src={iconeCamera} alt="" width="24" height="24" />
            {valores.imagemUrl ? 'Foto adicionada · 1 imagem' : 'Adicionar foto do problema'}
          </button>
          {valores.imagemUrl && (
            <>
              <img
                src={valores.imagemUrl}
                alt={`Foto ilustrativa: ${descricaoDaFoto(valores.imagemUrl)}`}
                className="md-foto-previa"
                style={{ height: 60 }}
              />
              <button type="button" className="md-botao-texto" onClick={() => mudar({ imagemUrl: '' })}>
                Remover foto
              </button>
            </>
          )}
          {erros.imagemUrl && (
            <p className="md-erro-campo" role="alert">
              {erros.imagemUrl}
            </p>
          )}
        </div>

        <Mensagem>{mensagem}</Mensagem>

        {vazio && !editando ? (
          <button type="button" className="md-botao md-botao-primario" onClick={preencherExemplo}>
            Preencher exemplo
          </button>
        ) : (
          <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
            {enviando ? 'Enviando…' : editando ? 'Salvar alterações' : 'Publicar denúncia'}
          </button>
        )}

        <p className="md-legenda">Não inclua rostos ou dados de outras pessoas.</p>
      </form>
    </Tela>
  );
}
