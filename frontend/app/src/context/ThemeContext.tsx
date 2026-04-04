import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  useCallback,
  ReactNode,
} from 'react';
import { useUserPreferences } from '../hooks/useUserPreferencesHooks';

type Theme = 'light' | 'dark';
type ThemePreference = 'light' | 'dark' | 'system';

interface ThemeContextType {
  theme: ThemePreference;
  effectiveTheme: Theme;
  setTheme: (theme: ThemePreference) => void;
}

const STORAGE_KEY = 'buurman-theme';

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function getSystemTheme(): Theme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';
}

function getStoredTheme(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored;
    }
  } catch {
    // localStorage unavailable
  }
  return 'system';
}

function applyThemeToDOM(effectiveTheme: Theme) {
  const root = document.documentElement;
  if (effectiveTheme === 'dark') {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
}

export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  const { data: preferences } = useUserPreferences();
  const [systemTheme, setSystemTheme] = useState<Theme>(getSystemTheme);
  const [themePreference, setThemePreference] =
    useState<ThemePreference>(getStoredTheme);

  // Sync from backend preferences when they load
  const prevBackendTheme = useRef<string | undefined>(undefined);
  useEffect(() => {
    if (
      preferences?.theme &&
      preferences.theme !== prevBackendTheme.current
    ) {
      prevBackendTheme.current = preferences.theme;
      const backendTheme = preferences.theme as ThemePreference;
      setThemePreference(backendTheme);
      try {
        localStorage.setItem(STORAGE_KEY, backendTheme);
      } catch {
        // ignore
      }
    }
  }, [preferences?.theme]);

  // Listen for system theme changes
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (e: MediaQueryListEvent) => {
      setSystemTheme(e.matches ? 'dark' : 'light');
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  const effectiveTheme: Theme =
    themePreference === 'system' ? systemTheme : themePreference;

  // Apply dark class to document immediately on change
  useEffect(() => {
    applyThemeToDOM(effectiveTheme);
  }, [effectiveTheme]);

  const setTheme = useCallback((newTheme: ThemePreference) => {
    setThemePreference(newTheme);
    try {
      localStorage.setItem(STORAGE_KEY, newTheme);
    } catch {
      // ignore
    }
  }, []);

  return (
    <ThemeContext.Provider
      value={{ theme: themePreference, effectiveTheme, setTheme }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
