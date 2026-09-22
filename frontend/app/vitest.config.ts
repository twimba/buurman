import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
  },
  resolve: {
    alias: [
      { find: '@/i18n', replacement: path.resolve(import.meta.dirname, './src/test/i18n.mock.ts') },
      { find: '@', replacement: path.resolve(import.meta.dirname, './src') },
    ],
  },
});
