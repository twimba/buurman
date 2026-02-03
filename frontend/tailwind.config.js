/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Modern Trust Color Scheme
        primary: {
          DEFAULT: '#1e3a8a', // Deep Navy
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#1e3a8a',
          600: '#1e40af',
          700: '#1e3a8a',
          800: '#1e3563',
          900: '#172554',
        },
        accent: {
          DEFAULT: '#10b981', // Emerald Green
          50: '#ecfdf5',
          100: '#d1fae5',
          200: '#a7f3d0',
          300: '#6ee7b7',
          400: '#34d399',
          500: '#10b981',
          600: '#059669',
          700: '#047857',
          800: '#065f46',
          900: '#064e3b',
        },
        background: {
          DEFAULT: '#f8fafc', // Soft Slate
          dark: '#f1f5f9',
        },
        surface: '#ffffff',
        text: {
          primary: '#1e293b',
          secondary: '#64748b', // Cool Gray
          muted: '#94a3b8',
        },
      },
    },
  },
}
