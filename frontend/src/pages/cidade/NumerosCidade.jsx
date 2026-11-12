import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../services/api.js';
import { numero } from '../../utils/formatos.js';

// último resumo recebido: ao voltar para o feed os números aparecem na hora (e a rolagem não pula)
let resumoEmCache = null;

/**
 * Faixa "Números da cidade" (pedido 8 do guia, sem tela no Figma): GET /relatorios/resumo.
 * Sem ?cidade=&uf=, a API usa a cidade do usuário logado. Enquanto carrega, a faixa já ocupa
 * a altura final (o feed não pula); se falhar, ela some. "Ver painel completo" abre /cidade/painel.
 */
export default function NumerosCidade() {
  const [resumo, setResumo] = useState(resumoEmCache);
  const [falhou, setFalhou] = useState(false);

  useEffect(() => {
    let ativo = true;
    api('/relatorios/resumo')
      .then((dados) => {
        resumoEmCache = dados;
        if (ativo) setResumo(dados);
      })
      .catch(() => ativo && !resumoEmCache && setFalhou(true));
    return () => {
      ativo = false;
    };
  }, []);

  if (falhou) return null;

  const valores = resumo
    ? [
        [numero(resumo.denuncias), 'denúncias'],
        [`${numero(Math.round(resumo.percentualResolvidas ?? 0))}%`, 'resolvidas'],
        [numero(resumo.usuarios), 'moradores'],
      ]
    : [[null, 'denúncias'], [null, 'resolvidas'], [null, 'moradores']];

  return (
    <section className="md-card md-numeros-cidade" aria-labelledby="numeros-cidade" aria-busy={!resumo || undefined}>
      <div className="md-numeros-cabecalho">
        <h2 id="numeros-cidade" className="md-numeros-titulo">
          Números da cidade
        </h2>
        <Link to="/cidade/painel" className="md-acao-texto">
          Ver painel completo
        </Link>
      </div>
      <dl className="md-numeros">
        {valores.map(([valor, rotulo]) => (
          <div key={rotulo} className="md-numero">
            <dt>{rotulo}</dt>
            <dd>{valor ?? <span className="md-esqueleto md-numero-esqueleto" aria-hidden="true" />}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
