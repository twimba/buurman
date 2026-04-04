import posthog from 'posthog-js';
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
  if (!analytics.isInitialized()) {
    return;
  }

  posthog.identify(userIdentifier, { role });
  posthog.group('team', teamIdentifier);
}
