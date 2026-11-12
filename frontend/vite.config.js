import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Checklist da FIAP: "o index.html abre direto no navegador" (file://, sem servidor).
 * Aberto por file://, o navegador recusa <script type="module"> e o atributo crossorigin.
 * Por isso o build gera um único script comum (IIFE) e este plugin ajusta as tags no HTML final.
 */
function abrirDiretoNoNavegador() {
  return {
    name: 'abrir-direto-no-navegador',
    apply: 'build',
    enforce: 'post',
    transformIndexHtml(html) {
      return html
        .replace(/<script type="module" crossorigin/g, '<script defer')
        .replace(/ crossorigin/g, '');
    },
  };
}

export default defineConfig({
  // Caminhos relativos: a pasta dist/ funciona em qualquer lugar (inclusive file://)
  base: './',
  plugins: [react(), abrirDiretoNoNavegador()],
  build: {
    // Um único script (exigido pelo file://) com React, Bootstrap e o app: ~520 kB, ~120 kB com gzip
    chunkSizeWarningLimit: 800,
    // CSS num arquivo próprio (<link> no index.html): a tela já aparece estilizada antes de o JS rodar
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        format: 'iife',
      },
    },
  },
});
