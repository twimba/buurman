import Keycloak from "keycloak-js";

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

// Keycloak initialization options — all pages are protected, always login-required
export const keycloakInitOptions = {
  onLoad: "login-required" as const,
  pkceMethod: "S256" as const,
  checkLoginIframe: true,
  silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  useNonce: false,
  enableLogging: import.meta.env.DEV,
};

export default keycloak;
