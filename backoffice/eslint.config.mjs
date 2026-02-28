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
    },
  },
  {
    ignores: ['dist/', 'build/', 'node_modules/', 'src/generated/', '*.config.js', '*.config.ts'],
  }
);
