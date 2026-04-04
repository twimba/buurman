import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { getBuildDefines } from '../scripts/vite-build-info'

const { defines } = getBuildDefines(process.env.npm_package_version || '0.0.0')

export default defineConfig({
  plugins: [react()],
  define: defines,
  server: {
    port: parseInt(process.env.VITE_DEV_PORT || '5174', 10),
    host: true,
    allowedHosts: true,
    hmr: {
      clientPort: parseInt(process.env.VITE_HMR_PORT || '443', 10),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: process.env.VITE_ENVIRONMENT === 'production' ? 'hidden' : true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/react-dom/') || id.includes('node_modules/react/') || id.includes('node_modules/react-router-dom/')) {
            return 'vendor-react';
          }
          if (id.includes('node_modules/@tanstack/react-query/') || id.includes('node_modules/axios/')) {
            return 'vendor-query';
          }
          if (id.includes('node_modules/posthog-js/')) {
            return 'vendor-analytics';
          }
          if (id.includes('node_modules/date-fns/') || id.includes('node_modules/keycloak-js/') || id.includes('node_modules/cronstrue/')) {
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
})
