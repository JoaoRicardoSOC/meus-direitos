import { useEffect, useState } from 'react';
import { api } from '../../services/api.js';
import { dataHora } from '../../utils/formatos.js';

/** Texto de cada passo: o registro, e depois a situação para a qual a denúncia foi. */
function rotulo(passo, indice) {
  if (passo.registro) return 'Denúncia registrada';
  if (passo.situacao?.codigo === 'ABERTA' && indice > 0) return 'Reaberta';
  return passo.situacao?.descricao ?? 'Situação atualizada';
}

/** Classe do marcador: a cor acompanha a situação (a mesma do selo do card). */
function classeDoPasso(passo) {
  if (passo.registro) return 'registro';
  return { EM_ANDAMENTO: 'andamento', RESOLVIDA: 'resolvida' }[passo.situacao?.codigo] ?? 'aberta';
}

/**
 * Linha do tempo da denúncia (GET /denuncias/{id}/historico), abaixo de "Já foi resolvido?".
 * Mostra quando foi registrada e cada mudança de situação, dizendo se foi o autor ou a comunidade.
 * `chave` muda quando a situação ou as confirmações mudam nesta tela: aí a linha é buscada de novo.
 * Se a busca falhar, a seção não aparece (o resto do detalhe continua útil).
 */
export default function LinhaDoTempo({ idDenuncia, chave }) {
  const [passos, setPassos] = useState(null);

  useEffect(() => {
    let ativo = true;
    api(`/denuncias/${idDenuncia}/historico`)
      .then((lista) => ativo && setPassos(Array.isArray(lista) ? lista : []))
      .catch(() => ativo && setPassos([]));
    return () => {
      ativo = false;
    };
  }, [idDenuncia, chave]);

  if (!passos || passos.length === 0) return null;

  return (
    <section className="md-card" aria-labelledby="linha-tempo-titulo">
      <h2 id="linha-tempo-titulo" className="md-negrito">
        Linha do tempo
      </h2>
      <ol className="md-linha-tempo">
        {passos.map((passo, i) => (
          <li key={`${passo.em}-${i}`} className={`md-linha-tempo-passo ${classeDoPasso(passo)}`}>
            <span className="md-linha-tempo-marcador" aria-hidden="true" />
            <span className="md-linha-tempo-texto">
              <span className="md-corpo md-negrito">{rotulo(passo, i)}</span>
              <span className="md-legenda">
                {passo.origem && <>{passo.origem.descricao} · </>}
                <time dateTime={passo.em}>{dataHora(passo.em)}</time>
              </span>
            </span>
          </li>
        ))}
      </ol>
    </section>
  );
}
