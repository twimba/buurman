import {
  createContext,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from 'react';
import { useUserPreferences } from '../hooks/useUserPreferencesHooks';
import { useAuth } from '../contexts/AuthContext';

type Theme = 'light' | 'dark';

interface ThemeContextType {
  theme: Theme;
  effectiveTheme: Theme;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

function getSystemTheme(): Theme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';
}

export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated } = useAuth();
  const { data: preferences } = useUserPreferences(isAuthenticated);
  const userTheme = preferences?.theme || 'system';
  const [systemTheme, setSystemTheme] = useState<Theme>(getSystemTheme);

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
    userTheme === 'system' ? systemTheme : (userTheme as Theme);

  // Apply dark class to document
  useEffect(() => {
    const root = document.documentElement;
    if (effectiveTheme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [effectiveTheme]);

  return (
    <ThemeContext.Provider
      value={{ theme: userTheme as Theme, effectiveTheme }}
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
