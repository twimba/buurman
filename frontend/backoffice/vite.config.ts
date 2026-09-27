import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { getBuildDefines } from '../scripts/vite-build-info';

const { defines } = getBuildDefines(process.env.npm_package_version || '0.0.0');

export default defineConfig({
  plugins: [
    react(),
    {
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
  ],
  define: defines,
  server: {
    // See the app config for why this reads LOCAL_BACKOFFICE_PORT rather than
    // the shared VITE_DEV_PORT (w3 -> 5204 / 3443, from .env).
    port: parseInt(
      process.env.LOCAL_BACKOFFICE_PORT || process.env.VITE_DEV_PORT || '5174',
      10
    ),
    strictPort: true,
    host: true,
    allowedHosts: true,
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
          if (id.includes('node_modules/posthog-js/')) {
            return 'vendor-analytics';
          }
          if (
            id.includes('node_modules/date-fns/') ||
            id.includes('node_modules/keycloak-js/') ||
            id.includes('node_modules/cronstrue/')
          ) {
            return 'vendor-utils';
          }
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
