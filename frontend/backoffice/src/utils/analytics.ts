import posthog from "posthog-js";
import type { AnalyticsEventName } from "../constants/analyticsEvents";

let initialized = false;

export function initAnalytics(apiKey: string, apiHost: string): void {
  if (!apiKey || initialized) {
    return;
  }

  posthog.init(apiKey, {
    api_host: apiHost,
    persistence: "memory",
    capture_pageview: true,
    autocapture: false,
    capture_pageleave: true,
    mask_all_text: false,
    mask_all_element_attributes: false,
    session_recording: {
      maskAllInputs: true,
      maskTextSelector: "[data-ph-mask]",
    },
  });

  posthog.register({ app_name: "backoffice" });

  initialized = true;
}

/**
 * Identify backoffice admin by Keycloak subject (UUID).
 * Backoffice uses a separate Keycloak realm (buurman-backoffice) with no
 * /users/me API, so the Keycloak sub claim is the only stable identifier.
 */
export function identifyUser(userSub: string): void {
  if (!initialized) {
    return;
  }

  posthog.identify(userSub);
}

export function resetAnalytics(): void {
  if (!initialized) {
    return;
  }

  posthog.reset();
}

export function trackEvent(
  event: AnalyticsEventName,
  properties?: Record<string, unknown>,
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
