/** Barra de etapas do cadastro e do questionário ("Etapa 1 de 4"). */
export default function Progresso({ etapa, total }) {
  return (
    <div className="d-flex flex-column" style={{ gap: 20 }}>
      <div
        className="md-progresso"
        role="progressbar"
        aria-valuemin={1}
        aria-valuemax={total}
        aria-valuenow={etapa}
        aria-label={`Etapa ${etapa} de ${total}`}
      >
        {Array.from({ length: total }, (_, i) => (
          <span key={i} className={`md-progresso-etapa ${i < etapa ? 'ativa' : ''}`} />
        ))}
      </div>
      <p className="md-legenda">
        Etapa {etapa} de {total}
      </p>
    </div>
  );
}
