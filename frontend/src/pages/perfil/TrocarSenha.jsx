import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Tela from '../../components/Tela.jsx';
import Cabecalho from '../../components/Cabecalho.jsx';
import CampoSenha from '../../components/CampoSenha.jsx';
import Mensagem from '../../components/Mensagem.jsx';
import { api } from '../../services/api.js';
import { obrigatorio, validar, validarCampos } from '../../utils/validacao.js';
import { focarPrimeiroErro } from '../../utils/foco.js';

const REGRAS = {
  senhaAtual: (v) => obrigatorio(v),
  novaSenha: (v, todos) =>
    validar.senha(v) || (v === todos.senhaAtual ? 'A nova senha deve ser diferente da atual.' : ''),
  confirmacao: (v, todos) => obrigatorio(v) || (v !== todos.novaSenha ? 'As senhas não são iguais.' : ''),
};

/**
 * Trocar senha (sem tela no Figma; segue o estilo da tela 20).
 * PUT /usuarios/me/senha → 204. Senha atual errada volta como 400 no campo, sem deslogar.
 */
export default function TrocarSenha() {
  const navegar = useNavigate();
  const [valores, setValores] = useState({ senhaAtual: '', novaSenha: '', confirmacao: '' });
  const [erros, setErros] = useState({});
  const [mensagem, setMensagem] = useState('');
  const [enviando, setEnviando] = useState(false);
  const formRef = useRef(null);

  function mudar(campo, valor) {
    setValores((v) => ({ ...v, [campo]: valor }));
    setErros((atual) => ({ ...atual, [campo]: undefined }));
    setMensagem('');
  }

  async function trocar(e) {
    e.preventDefault();
    setMensagem('');
    const encontrados = validarCampos(valores, REGRAS);
    setErros(encontrados);
    if (Object.keys(encontrados).length > 0) {
      setMensagem('Confira os campos destacados.');
      focarPrimeiroErro(formRef.current);
      return;
    }

    setEnviando(true);
    try {
      await api('/usuarios/me/senha', {
        metodo: 'PUT',
        corpo: { senhaAtual: valores.senhaAtual, novaSenha: valores.novaSenha },
      });
      navegar('/perfil', { replace: true, state: { aviso: 'Senha alterada com sucesso.' } });
    } catch (erro) {
      const porCampo = erro.porCampo();
      setErros(porCampo);
      if (!porCampo.senhaAtual && !porCampo.novaSenha) setMensagem(erro.message);
      else focarPrimeiroErro(formRef.current);
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Trocar senha">
      <Cabecalho titulo="Trocar senha" voltarPara="/perfil" />
      <div className="d-flex flex-column gap-2">
        <h2 className="md-titulo">Vamos trocar sua senha?</h2>
        <p className="md-corpo">Use pelo menos 8 caracteres, com letras e números.</p>
      </div>

      <form ref={formRef} className="md-grupo d-flex flex-column gap-3" onSubmit={trocar} noValidate>
        <Mensagem>{mensagem}</Mensagem>
        <CampoSenha
          rotulo="Senha atual"
          value={valores.senhaAtual}
          onChange={(e) => mudar('senhaAtual', e.target.value)}
          erro={erros.senhaAtual}
          autoComplete="current-password"
          maxLength={64}
        />
        <CampoSenha
          rotulo="Nova senha"
          value={valores.novaSenha}
          onChange={(e) => mudar('novaSenha', e.target.value)}
          erro={erros.novaSenha}
          autoComplete="new-password"
          maxLength={64}
        />
        <CampoSenha
          rotulo="Confirme a nova senha"
          value={valores.confirmacao}
          onChange={(e) => mudar('confirmacao', e.target.value)}
          erro={erros.confirmacao}
          autoComplete="new-password"
          maxLength={64}
        />

        <div className="md-grupo d-flex flex-column gap-3">
          <button type="submit" className="md-botao md-botao-primario" disabled={enviando}>
            {enviando ? 'Salvando…' : 'Salvar nova senha'}
          </button>
          <Link to="/perfil" className="md-botao md-botao-secundario">
            Cancelar
          </Link>
        </div>
      </form>
    </Tela>
  );
}
