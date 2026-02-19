import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { getBuildDefines } from '../scripts/vite-build-info'

const { defines } = getBuildDefines(process.env.npm_package_version || '0.0.0')

export default defineConfig({
  plugins: [react()],
  define: defines,
  server: {
    port: 5174,
    host: true,
    allowedHosts: ['backoffice.local.buurman.io'],
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
          'vendor-utils': ['date-fns', 'keycloak-js', 'cronstrue'],
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
