import Keycloak from 'keycloak-js';

// Create Keycloak instance with configuration
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
};

// Singleton pattern to ensure only one Keycloak instance is created
// This prevents "instance can only be initialized once" error in React StrictMode
let keycloakInstance: Keycloak | null = null;

const getKeycloakInstance = () => {
  if (!keycloakInstance) {
    keycloakInstance = new Keycloak(keycloakConfig);
  }
  return keycloakInstance;
};

const keycloak = getKeycloakInstance();

// Keycloak initialization options - only check SSO on protected pages
export const keycloakInitOptions = {
  pkceMethod: 'S256' as const,
  // Enable login iframe for silent SSO checks
  checkLoginIframe: true,
  silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
  // Disable nonce check to avoid validation errors in development
  useNonce: false,
  // Enable token refresh
  enableLogging: import.meta.env.DEV,
};

// Check if we should auto-check SSO (only on protected routes, not on public pages)
export const shouldCheckSso = () => {
  const publicPaths = ['/login', '/register', '/invitation'];
  return !publicPaths.some((path) => window.location.pathname.startsWith(path));
};

export default keycloak;
