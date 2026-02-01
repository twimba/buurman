import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import keycloak from '../config/keycloak';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: () => void;
  logout: () => void;
  token: string | undefined;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const tokenRefreshInterval = useRef<number | null>(null);
  const initStarted = useRef(false);

  useEffect(() => {
    // Prevent double initialization in React Strict Mode
    if (initStarted.current) {
      return;
    }

    initStarted.current = true;

    const initKeycloak = async () => {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
          pkceMethod: 'S256',
          checkLoginIframe: false,
        });

        console.log('Keycloak initialized. Authenticated:', authenticated);
        setIsAuthenticated(authenticated);
        setIsLoading(false);

        // Token refresh timer
        if (authenticated && !tokenRefreshInterval.current) {
          tokenRefreshInterval.current = setInterval(() => {
            keycloak.updateToken(70).catch(() => {
              console.error('Failed to refresh token');
              setIsAuthenticated(false);
              keycloak.logout();
            });
          }, 60000);
        }

        // Listen to token updates
        keycloak.onTokenExpired = () => {
          console.log('Token expired, attempting refresh');
          keycloak.updateToken(70).catch(() => {
            console.error('Failed to refresh expired token');
            setIsAuthenticated(false);
            keycloak.logout();
          });
        };

        keycloak.onAuthSuccess = () => {
          console.log('Authentication successful');
          setIsAuthenticated(true);
        };

        keycloak.onAuthError = () => {
          console.error('Authentication error');
          setIsAuthenticated(false);
        };

        keycloak.onAuthLogout = () => {
          console.log('Logged out');
          setIsAuthenticated(false);
        };

      } catch (error) {
        console.error('Keycloak init failed', error);
        setIsAuthenticated(false);
        setIsLoading(false);
      }
    };

    initKeycloak();

    // Cleanup on unmount
    return () => {
      if (tokenRefreshInterval.current) {
        clearInterval(tokenRefreshInterval.current);
        tokenRefreshInterval.current = null;
      }
    };
  }, []);

  const login = () => {
    keycloak.login();
  };

  const logout = () => {
    keycloak.logout({ redirectUri: window.location.origin });
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        login,
        logout,
        token: keycloak.token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
