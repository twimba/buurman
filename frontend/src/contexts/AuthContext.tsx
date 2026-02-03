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
  login: () => void;
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
  const initStarted = useRef(false);

  useEffect(() => {
    // Prevent double initialization in React StrictMode
    if (initStarted.current) {
      return;
    }
    initStarted.current = true;

    const initKeycloak = async () => {
      try {
        console.log('Initializing Keycloak...');
        // Only check SSO on protected pages, not on /login or /register
        const initOptions = shouldCheckSso()
          ? { ...keycloakInitOptions, onLoad: 'check-sso' as const }
          : keycloakInitOptions;
        const authenticated = await keycloak.init(initOptions);
        console.log('Keycloak initialized. Authenticated:', authenticated);

        setIsAuthenticated(authenticated);
        setToken(keycloak.token);
        setIsLoading(false);

        // Set up token refresh
        if (authenticated) {
          // Update token every time it's refreshed
          keycloak.onTokenExpired = () => {
            console.log('Token expired, refreshing...');
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
                  console.log('Token refreshed');
                  setToken(keycloak.token);
                }
              })
              .catch(() => {
                console.error('Failed to refresh token');
                setIsAuthenticated(false);
              });
          };

          // Update token state when it changes
          keycloak.onAuthSuccess = () => {
            console.log('Auth success');
            setIsAuthenticated(true);
            setToken(keycloak.token);
          };

          keycloak.onAuthLogout = () => {
            console.log('Auth logout');
            setIsAuthenticated(false);
            setToken(undefined);
          };
        }
      } catch (error) {
        console.error('Keycloak initialization failed:', error);
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const login = () => {
    // Check for pending invitation to redirect back after login
    const pendingInvitation = localStorage.getItem('pendingInvitation');
    const redirectUri = pendingInvitation
      ? `${window.location.origin}/invitation/${pendingInvitation}`
      : `${window.location.origin}/dashboard`;

    keycloak.login({
      redirectUri,
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
