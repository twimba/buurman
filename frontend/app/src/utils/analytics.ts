import posthog from 'posthog-js';
import type { AnalyticsEventName } from '../constants/analyticsEvents';

let initialized = false;

export function initAnalytics(apiKey: string, apiHost: string): void {
  if (!apiKey || initialized) {
    return;
  }

  posthog.init(apiKey, {
    api_host: apiHost,
    persistence: 'memory',
    capture_pageview: true,
    autocapture: false,
    capture_pageleave: true,
    mask_all_text: false,
    mask_all_element_attributes: false,
    session_recording: {
      maskAllInputs: true,
      maskTextSelector: '[data-ph-mask]',
    },
  });

  posthog.register({ app_name: 'app' });

  initialized = true;
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
  if (!initialized) {
    return;
  }

  posthog.identify(userIdentifier, { role });
  posthog.group('team', teamIdentifier);
}

export function resetAnalytics(): void {
  if (!initialized) {
    return;
  }

  posthog.reset();
}

export function trackEvent(
  event: AnalyticsEventName,
  properties?: Record<string, unknown>
): void {
  if (!initialized) {
    return;
  }

  try {
    posthog.capture(event, properties);
  } catch {
    // Silently ignore analytics errors
  }
}

/**
 * Suppress or resume analytics capture during admin impersonation sessions.
 * Wired into ImpersonationContext — called automatically on state changes.
 */
export function setImpersonating(active: boolean): void {
  if (!initialized) {
    return;
  }

  if (active) {
    posthog.opt_out_capturing();
  } else {
    posthog.opt_in_capturing();
  }
}
