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
    port: 5173,
    host: true,
    allowedHosts: ['app.local.buurman.io'],
    // HMR websocket goes through Traefik (wss://app.local.buurman.io:443)
    hmr: {
      clientPort: 443,
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-query': ['@tanstack/react-query', 'axios'],
          'vendor-recharts': ['recharts'],
          'vendor-tiptap': [
            '@tiptap/core',
            '@tiptap/react',
            '@tiptap/starter-kit',
            '@tiptap/extensions',
            '@tiptap/extension-font-family',
            '@tiptap/extension-highlight',
            '@tiptap/extension-link',
            '@tiptap/extension-placeholder',
            '@tiptap/extension-text-align',
            '@tiptap/extension-text-style',
            '@tiptap/extension-underline',
          ],
          'vendor-maps': ['@vis.gl/react-google-maps'],
          'vendor-utils': ['date-fns', 'keycloak-js', 'dompurify', 'libphonenumber-js'],
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
