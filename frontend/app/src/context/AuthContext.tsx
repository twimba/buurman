import { createAuthProvider } from '@buurman/ui';
import keycloak, {
  keycloakInitOptions,
  shouldCheckSso,
} from '../config/keycloak';

const { AuthProvider, useAuth } = createAuthProvider({
  keycloak,
  keycloakInitOptions,
  defaultRedirect: '/dashboard',
  shouldCheckSso,
});

export { AuthProvider, useAuth };
