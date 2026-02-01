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

// Keycloak initialization options
export const keycloakInitOptions = {
  // Check SSO to restore session on page reload
  onLoad: 'check-sso' as const,
  pkceMethod: 'S256' as const,
  checkLoginIframe: false,
  // Disable nonce check to avoid validation errors in development
  useNonce: false,
  // Enable token refresh
  enableLogging: import.meta.env.DEV,
};

export default keycloak;
