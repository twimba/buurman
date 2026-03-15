import { useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTeam } from '../context/TeamContext';
import { useCurrentUser } from '../hooks/useAuthHooks';
import { initAnalytics, identifyUser } from '../utils/analytics';
import { env } from '../config/env';

export function AnalyticsInitializer() {
  const { isAuthenticated } = useAuth();
  const { activeTeam } = useTeam();
  const { data: user } = useCurrentUser(isAuthenticated);

  useEffect(() => {
    const apiKey = env('VITE_POSTHOG_KEY');
    const apiHost = env('VITE_POSTHOG_HOST');
    initAnalytics(apiKey, apiHost);
  }, []);

  useEffect(() => {
    if (isAuthenticated && user && activeTeam) {
      identifyUser({
        userIdentifier: user.identifier,
        teamIdentifier: activeTeam.identifier,
        role: activeTeam.role,
      });
    }
  }, [isAuthenticated, user, activeTeam]);

  // resetAnalytics() is not called on logout because keycloak.logout()
  // navigates the browser before React can re-render. With persistence: 'memory',
  // all PostHog state is garbage collected on page unload anyway.

  return null;
}
