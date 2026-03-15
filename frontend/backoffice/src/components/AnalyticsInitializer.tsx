import { useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { initAnalytics, identifyUser } from "../utils/analytics";
import { env } from "../config/env";

export function AnalyticsInitializer() {
  const { isAuthenticated, keycloak } = useAuth();

  useEffect(() => {
    const apiKey = env("VITE_POSTHOG_KEY");
    const apiHost = env("VITE_POSTHOG_HOST");
    initAnalytics(apiKey, apiHost);
  }, []);

  useEffect(() => {
    if (isAuthenticated && keycloak.subject) {
      identifyUser(keycloak.subject);
    }
  }, [isAuthenticated, keycloak.subject]);

  // resetAnalytics() is not called on logout because keycloak.logout()
  // navigates the browser before React can re-render. With persistence: 'memory',
  // all PostHog state is garbage collected on page unload anyway.

  return null;
}
