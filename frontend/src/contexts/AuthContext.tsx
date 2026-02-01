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

// Flag to track if Keycloak has been initialized
let keycloakInitialized = false;

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const tokenRefreshInterval = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    // Prevent double initialization in React Strict Mode
    if (keycloakInitialized) {
      setIsAuthenticated(!!keycloak.authenticated);
      setIsLoading(false);
      return;
    }

    keycloakInitialized = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        setIsLoading(false);

        // Token refresh timer
        if (authenticated && !tokenRefreshInterval.current) {
          tokenRefreshInterval.current = setInterval(() => {
            keycloak.updateToken(70).catch(() => {
              console.error('Failed to refresh token');
              keycloak.logout();
            });
          }, 60000); // Check every 60 seconds
        }
      })
      .catch((error) => {
        console.error('Keycloak init failed', error);
        setIsLoading(false);
      });

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
