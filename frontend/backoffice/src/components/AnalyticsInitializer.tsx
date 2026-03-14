import { useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { initAnalytics, identifyUser, resetAnalytics } from '../utils/analytics';
import { env } from '../config/env';

export function AnalyticsInitializer() {
  const initRef = useRef(false);
  const { isAuthenticated, keycloak } = useAuth();

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
    if (isAuthenticated && keycloak.subject) {
      identifyUser(keycloak.subject);
    }
  }, [isAuthenticated, keycloak.subject]);

  useEffect(() => {
    if (!isAuthenticated) {
      resetAnalytics();
    }
  }, [isAuthenticated]);

  return null;
}
