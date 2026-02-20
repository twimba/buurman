import Keycloak from "keycloak-js";
import { env } from "./env";

// Create Keycloak instance with configuration
const keycloakConfig = {
  url: env("VITE_KEYCLOAK_URL"),
  realm: env("VITE_BACKOFFICE_KEYCLOAK_REALM"),
  clientId: env("VITE_BACKOFFICE_KEYCLOAK_CLIENT_ID"),
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
  // Disable login iframe — blocked by third-party cookie restrictions in modern browsers
  checkLoginIframe: false,
  silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  useNonce: false,
  enableLogging: import.meta.env.DEV,
};

export default keycloak;
