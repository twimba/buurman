import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useRef,
} from 'react';
import type Keycloak from 'keycloak-js';

export interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (redirectUri?: string, loginHint?: string) => void;
  logout: () => void;
  token: string | undefined;
  keycloak: Keycloak;
}

export interface AuthProviderOptions {
  keycloak: Keycloak;
  keycloakInitOptions: object;
  defaultRedirect: string;
  shouldCheckSso?: () => boolean;
}

export function createAuthProvider(options: AuthProviderOptions) {
  const { keycloak, keycloakInitOptions, defaultRedirect, shouldCheckSso } =
    options;

  const AuthContext = createContext<AuthContextType | undefined>(undefined);

  const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
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
          const initOptions =
            shouldCheckSso && shouldCheckSso()
              ? { ...keycloakInitOptions, onLoad: 'check-sso' as const }
              : keycloakInitOptions;

          const authenticated = await keycloak.init(initOptions);

          setIsAuthenticated(authenticated);
          setToken(keycloak.token);
          setIsLoading(false);

          // Set up token refresh
          if (authenticated) {
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
      const locale = localStorage.getItem('buurman-language') || 'en';
      keycloak.login({
        redirectUri:
          redirectUri || `${window.location.origin}${defaultRedirect}`,
        locale,
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
        <div
          style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
          }}
        >
          <div style={{ textAlign: 'center', maxWidth: '28rem' }}>
            <h1
              style={{
                fontSize: '1.25rem',
                fontWeight: 600,
                marginBottom: '0.5rem',
              }}
            >
              Service Unavailable
            </h1>
            <p style={{ color: '#6b7280', marginBottom: '1rem' }}>
              {initError}
            </p>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '0.5rem 1rem',
                background: '#3b82f6',
                color: 'white',
                border: 'none',
                borderRadius: '0.5rem',
                cursor: 'pointer',
              }}
            >
              Retry
            </button>
          </div>
        </div>
      );
    }

    return (
      <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
    );
  };

  const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
      throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
  };

  return { AuthProvider, useAuth };
}
