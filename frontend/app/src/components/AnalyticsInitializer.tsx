import { useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTeam } from '../context/TeamContext';
import { useCurrentUser } from '../hooks/useAuthHooks';
import { initAnalytics, identifyUser, resetAnalytics } from '../utils/analytics';
import { env } from '../config/env';

export function AnalyticsInitializer() {
  const initRef = useRef(false);
  const { isAuthenticated } = useAuth();
  const { activeTeam } = useTeam();
  const { data: user } = useCurrentUser(isAuthenticated);

  useEffect(() => {
    if (initRef.current) {
      return;
    }
    initRef.current = true;

    const apiKey = env('VITE_POSTHOG_KEY');
    const apiHost = env('VITE_POSTHOG_HOST');
    initAnalytics(apiKey, apiHost);
  }, []);

  useEffect(() => {
    if (isAuthenticated && user && activeTeam) {
      identifyUser({
        userIdentifier: user.identifier,
        teamIdentifier: activeTeam.identifier,
        teamName: activeTeam.name,
        role: activeTeam.role,
      });
    }
  }, [isAuthenticated, user, activeTeam]);

  useEffect(() => {
    if (!isAuthenticated) {
      resetAnalytics();
    }
  }, [isAuthenticated]);

  return null;
}
