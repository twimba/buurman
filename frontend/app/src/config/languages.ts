/**
 * The languages the product is translated into, in the same order as the backend's
 * DocumentLanguages.ORDERED. This is the frontend's single source of truth: i18next's
 * supportedLngs and the locale-parity test both read it, so the list cannot drift.
 *
 * This deliberately does not live under src/i18n/: vitest.config.ts aliases '@/i18n' to a test
 * mock, and Vite string aliases match by prefix, so an '@/i18n/languages' import would resolve
 * into that mock inside any component test.
 */
export const SUPPORTED_LANGUAGES = [
  'en',
  'nl',
  'de',
  'fr',
  'pt',
  'es',
  'sv',
  'it',
  'fi',
  'el',
  'pl',
  'da',
  'nb',
] as const;

export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];
