import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import HttpBackend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(HttpBackend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: ['en', 'nl', 'pt', 'es', 'fr', 'de', 'it', 'sv', 'fi', 'el', 'pl', 'da', 'nb'],
    ns: [
      'common',
      'navigation',
      'properties',
      'tenants',
      'contracts',
      'payments',
      'expenses',
      'documents',
      'settings',
      'admin',
    ],
    defaultNS: 'common',
    backend: {
      loadPath: '/locales/{{lng}}/{{ns}}.json',
    },
    detection: {
      order: ['localStorage'],
      lookupLocalStorage: 'buurman-language',
      caches: ['localStorage'],
    },
    interpolation: {
      escapeValue: false,
    },
    react: {
      useSuspense: false,
    },
  });

export default i18n;
