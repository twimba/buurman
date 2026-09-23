import { createAnalytics } from '@buurman/ui';
import type { AnalyticsEventName } from '../constants/analyticsEvents';

const analytics = createAnalytics('app');

export const initAnalytics = analytics.initAnalytics;
export const resetAnalytics = analytics.resetAnalytics;
export const setImpersonating = analytics.setImpersonating;

export function trackEvent(
  event: AnalyticsEventName,
  properties?: Record<string, unknown>
): void {
  analytics.trackEvent(event, properties);
}

export function identifyUser({
  userIdentifier,
  teamIdentifier,
  role,
}: {
  userIdentifier: string;
  teamIdentifier: string;
  role: string;
}): void {
  // No isInitialized() guard: posthog-js is loaded lazily, and the facade
  // queues these until it resolves so identification is never dropped.
  analytics.identify(userIdentifier, { role });
  analytics.group('team', teamIdentifier);
}
