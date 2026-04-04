import { createAuthProvider } from '@buurman/ui';
import keycloak, { keycloakInitOptions } from '../config/keycloak';

const { AuthProvider, useAuth } = createAuthProvider({
  keycloak,
  keycloakInitOptions,
  defaultRedirect: '/teams',
});

export { AuthProvider, useAuth };
