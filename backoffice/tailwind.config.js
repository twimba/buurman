/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../packages/ui/src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#5c7cfa',
          50: '#f0f4ff',
          100: '#dbe4ff',
          200: '#bac8ff',
          300: '#91a7ff',
          400: '#748ffc',
          500: '#5c7cfa',
          600: '#4c6ef5',
          700: '#4263eb',
          800: '#3b5bdb',
          900: '#364fc7',
        },
        accent: {
          DEFAULT: '#fcc419',
          50: '#fff9db',
          100: '#fff3bf',
          200: '#ffec99',
          300: '#ffe066',
          400: '#ffd43b',
          500: '#fcc419',
          600: '#fab005',
          700: '#f59f00',
          800: '#e67700',
          900: '#d9480f',
        },
        background: {
          DEFAULT: '#f8f9fc',
          dark: '#0c0d14',
        },
        surface: '#ffffff',
        text: {
          primary: '#1a1d2e',
          secondary: '#6b7194',
          muted: '#9ca0b8',
        },
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
      },
      boxShadow: {
        'sm': '0 1px 3px rgba(26, 29, 46, 0.06), 0 1px 2px rgba(26, 29, 46, 0.04)',
        'DEFAULT': '0 1px 3px rgba(26, 29, 46, 0.08), 0 1px 2px rgba(26, 29, 46, 0.06)',
        'md': '0 4px 6px -1px rgba(26, 29, 46, 0.06), 0 2px 4px -2px rgba(26, 29, 46, 0.04)',
        'lg': '0 10px 15px -3px rgba(26, 29, 46, 0.06), 0 4px 6px -4px rgba(26, 29, 46, 0.04)',
        'xl': '0 20px 25px -5px rgba(26, 29, 46, 0.08), 0 8px 10px -6px rgba(26, 29, 46, 0.04)',
      },
    },
  },
}
