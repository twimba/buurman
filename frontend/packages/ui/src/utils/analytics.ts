import posthog from 'posthog-js';

export function createAnalytics(appName: string) {
  let initialized = false;

  function initAnalytics(apiKey: string, apiHost: string): void {
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

    posthog.register({ app_name: appName });

    initialized = true;
  }

  function resetAnalytics(): void {
    if (!initialized) {
      return;
    }

    posthog.reset();
  }

  function trackEvent(
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

  /**
   * Suppress or resume analytics capture during admin impersonation sessions.
   * Wired into ImpersonationContext -- called automatically on state changes.
   */
  function setImpersonating(active: boolean): void {
    if (!initialized) {
      return;
    }

    if (active) {
      posthog.opt_out_capturing();
    } else {
      posthog.opt_in_capturing();
    }
  }

  function isInitialized(): boolean {
    return initialized;
  }

  return {
    initAnalytics,
    resetAnalytics,
    trackEvent,
    setImpersonating,
    isInitialized,
  };
}
