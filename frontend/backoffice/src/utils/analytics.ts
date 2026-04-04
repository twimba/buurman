import posthog from 'posthog-js';
import { createAnalytics } from '@buurman/ui';
import type { AnalyticsEventName } from '../constants/analyticsEvents';

const analytics = createAnalytics('backoffice');

export const initAnalytics = analytics.initAnalytics;
export const resetAnalytics = analytics.resetAnalytics;

export function trackEvent(
  event: AnalyticsEventName,
  properties?: Record<string, unknown>
): void {
  analytics.trackEvent(event, properties);
}

/**
 * Identify backoffice admin by Keycloak subject (UUID).
 * Backoffice uses a separate Keycloak realm (buurman-backoffice) with no
 * /users/me API, so the Keycloak sub claim is the only stable identifier.
 */
export function identifyUser(userSub: string): void {
  if (!analytics.isInitialized()) {
    return;
  }

  posthog.identify(userSub);
}
