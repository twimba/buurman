import {
  createContext,
  useContext,
  useEffect,
  useCallback,
  ReactNode,
} from 'react';
import { useTranslation } from 'react-i18next';
import {
  useUserPreferences,
  useUpdateUserPreferences,
} from '../hooks/useUserPreferencesHooks';

export const supportedLanguages = [
  { value: 'en', label: 'English' },
  { value: 'nl', label: 'Nederlands' },
  { value: 'pt', label: 'Português' },
  { value: 'es', label: 'Español' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'it', label: 'Italiano' },
  { value: 'sv', label: 'Svenska' },
  { value: 'fi', label: 'Suomi' },
  { value: 'el', label: 'Ελληνικά' },
  { value: 'pl', label: 'Polski' },
  { value: 'da', label: 'Dansk' },
  { value: 'nb', label: 'Norsk' },
] as const;

interface LocaleContextType {
  locale: string;
  setLocale: (lang: string) => void;
}

const STORAGE_KEY = 'buurman-language';

const LocaleContext = createContext<LocaleContextType | undefined>(undefined);

export const LocaleProvider = ({ children }: { children: ReactNode }) => {
  const { i18n } = useTranslation();
  const { data: preferences } = useUserPreferences();
  const updatePreferencesMutation = useUpdateUserPreferences();

  // Sync i18n language from user preferences when they load
  useEffect(() => {
    if (preferences?.language && preferences.language !== i18n.language) {
      i18n.changeLanguage(preferences.language);
      try {
        localStorage.setItem(STORAGE_KEY, preferences.language);
      } catch {
        // ignore
      }
    }
  }, [preferences?.language, i18n]);

  const setLocale = useCallback(
    (lang: string) => {
      i18n.changeLanguage(lang);
      try {
        localStorage.setItem(STORAGE_KEY, lang);
      } catch {
        // ignore
      }
      updatePreferencesMutation.mutate({ language: lang });
    },
    [i18n, updatePreferencesMutation]
  );

  return (
    <LocaleContext.Provider value={{ locale: i18n.language, setLocale }}>
      {children}
    </LocaleContext.Provider>
  );
};

export const useLocale = () => {
  const context = useContext(LocaleContext);
  if (context === undefined) {
    throw new Error('useLocale must be used within a LocaleProvider');
  }
  return context;
};
