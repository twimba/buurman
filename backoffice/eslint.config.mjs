import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import { fixupConfigRules, fixupPluginRules } from '@eslint/compat';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';

export default tseslint.config(
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  ...fixupConfigRules([
    {
      ...reactPlugin.configs.flat.recommended,
      settings: {
        react: {
          version: 'detect',
        },
      },
    },
    reactPlugin.configs.flat['jsx-runtime'],
  ]),
  {
    plugins: {
      'react-hooks': fixupPluginRules(reactHooksPlugin),
    },
    rules: {
      ...reactHooksPlugin.configs.recommended.rules,
    },
  },
  {
    rules: {
      'react/prop-types': 'off', // Using TypeScript for type checking
      '@typescript-eslint/no-non-null-assertion': 'warn',
      'no-restricted-syntax': ['warn', {
        selector: 'Literal[value=/\\[#[0-9a-fA-F]{3,8}\\]/]',
        message: 'Use design tokens instead of hardcoded hex values in Tailwind classes. See docs/DESIGN_SYSTEM.md.',
      }],
    },
  },
  {
    ignores: ['dist/', 'build/', 'node_modules/', 'src/generated/', '*.config.js', '*.config.ts'],
  }
);
