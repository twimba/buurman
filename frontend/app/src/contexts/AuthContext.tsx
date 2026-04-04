import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useRef,
} from 'react';
import keycloak, {
  keycloakInitOptions,
  shouldCheckSso,
} from '../config/keycloak';
import type Keycloak from 'keycloak-js';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (redirectUri?: string, loginHint?: string) => void;
  logout: () => void;
  token: string | undefined;
  keycloak: Keycloak;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Custom provider that's React 18 StrictMode compatible
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [token, setToken] = useState<string | undefined>(undefined);
  const [initError, setInitError] = useState<string | null>(null);
  const initStarted = useRef(false);

  useEffect(() => {
    // Prevent double initialization in React StrictMode
    if (initStarted.current) {
      return;
    }
    initStarted.current = true;

    const initKeycloak = async () => {
      try {
        // Only check SSO on protected pages, not on /login or /register
        const initOptions = shouldCheckSso()
          ? { ...keycloakInitOptions, onLoad: 'check-sso' as const }
          : keycloakInitOptions;
        const authenticated = await keycloak.init(initOptions);

        setIsAuthenticated(authenticated);
        setToken(keycloak.token);
        setIsLoading(false);

        // Set up token refresh
        if (authenticated) {
          // Update token every time it's refreshed
          keycloak.onTokenExpired = () => {
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
                  setToken(keycloak.token);
                }
              })
              .catch(() => {
                setIsAuthenticated(false);
              });
          };

          keycloak.onAuthSuccess = () => {
            setIsAuthenticated(true);
            setToken(keycloak.token);
          };

          keycloak.onAuthLogout = () => {
            setIsAuthenticated(false);
            setToken(undefined);
          };
        }
      } catch {
        setInitError(
          'Authentication service is currently unavailable. Please try again later.'
        );
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const login = (redirectUri?: string, loginHint?: string) => {
    keycloak.login({
      redirectUri: redirectUri || `${window.location.origin}/dashboard`,
      ...(loginHint && { loginHint }),
    });
  };

  const logout = () => {
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  };

  const value: AuthContextType = {
    isAuthenticated,
    isLoading,
    login,
    logout,
    token,
    keycloak,
  };

  if (initError) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="text-center max-w-md">
          <h1 className="text-xl font-semibold text-text-primary mb-2">
            Service Unavailable
          </h1>
          <p className="text-text-secondary mb-4">{initError}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom hook to use auth context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
