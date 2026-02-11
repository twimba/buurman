import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useRef,
} from "react";
import keycloak, { keycloakInitOptions } from "../config/keycloak";
import type Keycloak from "keycloak-js";

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (redirectUri?: string) => void;
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
        // All backoffice pages are protected — always login-required
        const authenticated = await keycloak.init(keycloakInitOptions);

        setIsAuthenticated(authenticated);
        setToken(keycloak.token);
        setIsLoading(false);

        // Set up token refresh
        if (authenticated) {
          keycloak.onTokenExpired = () => {
            console.log("Token expired, refreshing...");
            keycloak
              .updateToken(30)
              .then((refreshed) => {
                if (refreshed) {
                  console.log("Token refreshed");
                  setToken(keycloak.token);
                }
              })
              .catch(() => {
                console.error("Failed to refresh token");
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
      } catch (error) {
        console.error("Keycloak initialization failed:", error);
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const login = (redirectUri?: string) => {
    keycloak.login({
      redirectUri: redirectUri || `${window.location.origin}/teams`,
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
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
};
