import marca from '../assets/icones/marca.svg';
import ilustracao from '../assets/imagens/comunidade.svg';

/**
 * Painel ao lado do app em telas largas (1100px ou mais, ver tema.css): apresenta o projeto
 * quando o protótipo é aberto num computador. No celular e no tablet ele não aparece.
 */
export default function PainelApresentacao() {
  return (
    <aside className="md-painel" aria-label="Sobre o projeto">
      <div className="d-flex align-items-center gap-2">
        <img src={marca} alt="" width="32" height="32" />
        <span className="md-subtitulo" style={{ color: 'var(--md-verde)' }}>
          meus direitos
        </span>
      </div>
      <div style={{ background: 'var(--md-verde)', borderRadius: 28, overflow: 'hidden' }} aria-hidden="true">
        <img src={ilustracao} alt="" width="360" height="210" style={{ display: 'block', width: '100%', height: 'auto' }} />
      </div>
      <p className="md-titulo">Sua voz melhora a cidade. Seus direitos abrem caminhos.</p>
      <p className="md-corpo">
        Moradores registram problemas do bairro, votam, conversam e confirmam juntos quando algo foi resolvido. Um
        questionário opcional indica possibilidades de apoio e os canais oficiais para confirmá-las.
      </p>
      <p className="md-legenda">
        ODS 10 · Redução das desigualdades · ODS 11 · Cidades e comunidades sustentáveis
        <br />
        Protótipo acadêmico · FIAP Global Solution 2026
      </p>
    </aside>
  );
}
