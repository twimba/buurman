import posthog from 'posthog-js';

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

  initialized = true;
}

export function identifyUser({
  userIdentifier,
  teamIdentifier,
  teamName,
  role,
}: {
  userIdentifier: string;
  teamIdentifier: string;
  teamName: string;
  role: string;
}): void {
  if (!initialized) {
    return;
  }

  posthog.identify(userIdentifier, { role });
  posthog.group('team', teamIdentifier, { name: teamName });
}

export function resetAnalytics(): void {
  if (!initialized) {
    return;
  }

  posthog.reset();
}

export function trackEvent(
  event: string,
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
