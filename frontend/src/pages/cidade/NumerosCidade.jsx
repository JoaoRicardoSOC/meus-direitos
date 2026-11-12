import { useEffect, useState } from 'react';
import { api } from '../../services/api.js';

const porcentagem = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
const numero = new Intl.NumberFormat('pt-BR');

/**
 * Bloco "Números da cidade" (pedido 8 do guia, sem tela no Figma): GET /relatorios/resumo.
 * Sem ?cidade=&uf=, a API usa a cidade do usuário logado. Se falhar, o bloco simplesmente não aparece.
 */
export default function NumerosCidade() {
  const [resumo, setResumo] = useState(null);

  useEffect(() => {
    let ativo = true;
    api('/relatorios/resumo')
      .then((dados) => ativo && setResumo(dados))
      .catch(() => ativo && setResumo(null));
    return () => {
      ativo = false;
    };
  }, []);

  if (!resumo) return null;

  return (
    <section className="md-card gap-2" aria-labelledby="numeros-cidade">
      <h2 id="numeros-cidade" className="md-sobretitulo">
        Números da cidade
      </h2>
      <dl className="md-numeros">
        <div className="md-numero">
          <dt>denúncias</dt>
          <dd>{numero.format(resumo.denuncias ?? 0)}</dd>
        </div>
        <div className="md-numero">
          <dt>resolvidas</dt>
          <dd>{porcentagem.format(resumo.percentualResolvidas ?? 0)}%</dd>
        </div>
        <div className="md-numero">
          <dt>moradores</dt>
          <dd>{numero.format(resumo.usuarios ?? 0)}</dd>
        </div>
      </dl>
    </section>
  );
}
