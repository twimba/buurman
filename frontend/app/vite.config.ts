import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { writeFileSync, mkdirSync } from 'fs'
import { getBuildDefines } from '../scripts/vite-build-info'

const { defines, raw } = getBuildDefines(process.env.npm_package_version || '0.0.0')

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'generate-build-info',
      configureServer(server) {
        server.middlewares.use('/build-info.json', (_req, res) => {
          res.setHeader('Content-Type', 'application/json')
          res.setHeader('Access-Control-Allow-Origin', '*')
          res.end(JSON.stringify(raw))
        })
      },
      closeBundle() {
        mkdirSync('dist', { recursive: true })
        writeFileSync('dist/build-info.json', JSON.stringify(raw))
      },
    },
  ],
  define: defines,
  server: {
    port: parseInt(process.env.VITE_DEV_PORT || '5173', 10),
    host: true,
    allowedHosts: true,
    // HMR websocket goes through Traefik (wss://app.local.buurman.io:443)
    hmr: {
      clientPort: parseInt(process.env.VITE_HMR_PORT || '443', 10),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    chunkSizeWarningLimit: 1300,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/react-dom/') || id.includes('node_modules/react/') || id.includes('node_modules/react-router-dom/')) {
            return 'vendor-react';
          }
          if (id.includes('node_modules/@tanstack/react-query/') || id.includes('node_modules/axios/')) {
            return 'vendor-query';
          }
          if (id.includes('node_modules/recharts/')) {
            return 'vendor-recharts';
          }
          if (id.includes('node_modules/@tiptap/')) {
            return 'vendor-tiptap';
          }
          if (id.includes('node_modules/@fullcalendar/')) {
            return 'vendor-fullcalendar';
          }
          if (id.includes('node_modules/@vis.gl/react-google-maps/')) {
            return 'vendor-maps';
          }
          if (id.includes('node_modules/posthog-js/')) {
            return 'vendor-analytics';
          }
          if (id.includes('node_modules/date-fns/') || id.includes('node_modules/keycloak-js/') || id.includes('node_modules/dompurify/') || id.includes('node_modules/libphonenumber-js/')) {
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
      'ical.js': path.resolve(__dirname, '../node_modules/ical.js/dist/ical.es5.cjs'),
    },
  },
})
