import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { writeFileSync, mkdirSync } from 'fs';
import { getBuildDefines } from '../scripts/vite-build-info';

const { defines, raw } = getBuildDefines(
  process.env.npm_package_version || '0.0.0'
);

export default defineConfig({
  plugins: [
    react(),
    {
      // config.js is a runtime-injected Docker config — not a Vite module.
      // Remove it before Vite's HTML processor sees it (which warns about non-module
      // scripts), then inject it back via the tags API so Vite leaves it alone.
      name: 'runtime-config-script',
      transformIndexHtml: {
        order: 'pre',
        handler(html) {
          return {
            html: html.replace('<script src="/config.js"></script>', ''),
            tags: [
              {
                tag: 'script',
                attrs: { src: '/config.js' },
                injectTo: 'body-prepend',
              },
            ],
          };
        },
      },
    },
    {
      name: 'generate-build-info',
      configureServer(server) {
        server.middlewares.use('/build-info.json', (_req, res) => {
          res.setHeader('Content-Type', 'application/json');
          res.setHeader('Access-Control-Allow-Origin', '*');
          res.end(JSON.stringify(raw));
        });
      },
      closeBundle() {
        mkdirSync('dist', { recursive: true });
        writeFileSync('dist/build-info.json', JSON.stringify(raw));
      },
    },
  ],
  define: defines,
  server: {
    // LOCAL_APP_PORT and HTTPS_PORT come from .env, which setup-workspace.sh
    // regenerates per workspace (w3 -> 5203 / 3443). They are read here because
    // VITE_DEV_PORT is shared with the backoffice config, so a single value
    // would put both dev servers on the same port.
    port: parseInt(
      process.env.LOCAL_APP_PORT || process.env.VITE_DEV_PORT || '5173',
      10
    ),
    // Fail loudly instead of silently drifting to the next free port -- a dev
    // server on an unexpected port is invisible to Traefik and to the hub.
    strictPort: true,
    host: true,
    allowedHosts: true,
    // HMR websocket goes through Traefik (wss://w<N>-app.local.buurman.io:<HTTPS_PORT>)
    hmr: {
      clientPort: parseInt(
        process.env.VITE_HMR_PORT || process.env.HTTPS_PORT || '443',
        10
      ),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: process.env.VITE_ENVIRONMENT === 'production' ? 'hidden' : true,
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (
            id.includes('node_modules/react-dom/') ||
            id.includes('node_modules/react/') ||
            id.includes('node_modules/react-router-dom/')
          ) {
            return 'vendor-react';
          }
          if (
            id.includes('node_modules/@tanstack/react-query/') ||
            id.includes('node_modules/axios/')
          ) {
            return 'vendor-query';
          }
          if (id.includes('node_modules/recharts/')) {
            return 'vendor-recharts';
          }
          if (id.includes('node_modules/@tiptap/')) {
            return 'vendor-tiptap';
          }
          if (id.includes('node_modules/@vis.gl/react-google-maps/')) {
            return 'vendor-maps';
          }
          if (id.includes('node_modules/posthog-js/')) {
            return 'vendor-analytics';
          }
          if (
            id.includes('node_modules/i18next') ||
            id.includes('node_modules/react-i18next')
          ) {
            return 'vendor-i18n';
          }
          // Kept out of vendor-utils: that chunk is eager because keycloak-js
          // is needed at startup, while phone parsing is only reached from
          // lazy form routes. Its metadata is large enough to be worth its own
          // chunk rather than riding along on every first visit.
          if (id.includes('node_modules/libphonenumber-js/')) {
            return 'vendor-phone';
          }
          if (
            id.includes('node_modules/date-fns/') ||
            id.includes('node_modules/keycloak-js/') ||
            id.includes('node_modules/dompurify/')
          ) {
            return 'vendor-utils';
          }
        },
      },
    },
  },
  optimizeDeps: {
    include: ['ical.js'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      // ical.js v2 only exports a default — point to the CJS build so
      // esbuild can synthesise named exports and suppress IMPORT_IS_UNDEFINED warnings
      'ical.js': path.resolve(
        __dirname,
        '../node_modules/ical.js/dist/ical.es5.cjs'
      ),
    },
  },
});
