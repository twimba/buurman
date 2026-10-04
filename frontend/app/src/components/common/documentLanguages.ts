export type DocumentLanguageCode =
  | 'en'
  | 'nl'
  | 'de'
  | 'es'
  | 'fr'
  | 'pt'
  | 'it'
  | 'sv'
  | 'fi'
  | 'el'
  | 'pl'
  | 'da'
  | 'nb';

export interface DocumentLanguage {
  code: DocumentLanguageCode;
  /** Endonym, so every user can find their own language whatever the UI language is. */
  label: string;
}

/** PDF languages, in menu order. The active UI language is pinned to the top at render. */
export const DOCUMENT_LANGUAGES: DocumentLanguage[] = [
  { code: 'en', label: 'English' },
  { code: 'nl', label: 'Nederlands' },
  { code: 'de', label: 'Deutsch' },
  { code: 'es', label: 'Español' },
  { code: 'fr', label: 'Français' },
  { code: 'pt', label: 'Português' },
  { code: 'it', label: 'Italiano' },
  { code: 'sv', label: 'Svenska' },
  { code: 'fi', label: 'Suomi' },
  { code: 'el', label: 'Ελληνικά' },
  { code: 'pl', label: 'Polski' },
  { code: 'da', label: 'Dansk' },
  { code: 'nb', label: 'Norsk' },
];

/** The base language of an i18next language tag ("pt-BR" -> "pt"). */
export const baseLanguage = (i18nLanguage: string): string =>
  i18nLanguage.split('-')[0];

/**
 * Active UI language first, then the rest in canonical order. `allowed` limits the list to those
 * codes (every language when omitted).
 */
export const orderDocumentLanguages = (
  uiLanguage: string,
  allowed?: readonly string[]
): DocumentLanguage[] => {
  const languages = allowed
    ? DOCUMENT_LANGUAGES.filter((l) => allowed.includes(l.code))
    : DOCUMENT_LANGUAGES;
  const current = languages.find((l) => l.code === uiLanguage);
  const rest = languages.filter((l) => l.code !== uiLanguage);
  return current ? [current, ...rest] : languages;
};

export const isDocumentLanguageCode = (
  code: string
): code is DocumentLanguageCode =>
  DOCUMENT_LANGUAGES.some((l) => l.code === code);

/** The UI language when documents can be rendered in it, otherwise English. */
export const defaultDocumentLanguage = (
  i18nLanguage: string
): DocumentLanguageCode =>
  DOCUMENT_LANGUAGES.find((l) => l.code === baseLanguage(i18nLanguage))?.code ??
  'en';

export const documentLanguageLabel = (code: DocumentLanguageCode): string =>
  DOCUMENT_LANGUAGES.find((l) => l.code === code)?.label ?? code;
