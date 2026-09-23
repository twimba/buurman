type PostHog = typeof import('posthog-js').default;

/**
 * Analytics facade over PostHog.
 *
 * posthog-js is ~92 KB gzipped and is loaded through a dynamic import so it
 * never sits on the critical path: telemetry must not compete with the app's
 * own startup for bandwidth. Calls made before the library resolves are queued
 * and replayed on load, so no events are lost during the gap and callers never
 * have to care whether the client is ready.
 */
export function createAnalytics(appName: string) {
  let client: PostHog | null = null;
  let loading: Promise<void> | null = null;
  const pending: Array<(posthog: PostHog) => void> = [];

  /** Runs now if the client is ready, otherwise replays once it loads. */
  function withClient(action: (posthog: PostHog) => void): void {
    if (client) {
      action(client);
    } else if (loading) {
      pending.push(action);
    }
    // No init attempted (no API key): drop silently, analytics is disabled.
  }

  function initAnalytics(apiKey: string, apiHost: string): void {
    if (!apiKey || loading) {
      return;
    }

    loading = import('posthog-js')
      .then(({ default: posthog }) => {
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

        client = posthog;
        pending.splice(0).forEach((action) => {
          try {
            action(posthog);
          } catch {
            // Ignore replay failures; analytics must never break the app.
          }
        });
      })
      .catch(() => {
        // Analytics is optional — a blocked or failed load is not an app error.
        pending.length = 0;
      });
  }

  function resetAnalytics(): void {
    withClient((posthog) => posthog.reset());
  }

  function trackEvent(
    event: string,
    properties?: Record<string, unknown>
  ): void {
    withClient((posthog) => {
      try {
        posthog.capture(event, properties);
      } catch {
        // Silently ignore analytics errors
      }
    });
  }

  function identify(
    distinctId: string,
    properties?: Record<string, unknown>
  ): void {
    withClient((posthog) => posthog.identify(distinctId, properties));
  }

  function group(groupType: string, groupKey: string): void {
    withClient((posthog) => posthog.group(groupType, groupKey));
  }

  /**
   * Suppress or resume analytics capture during admin impersonation sessions.
   * Wired into ImpersonationContext -- called automatically on state changes.
   */
  function setImpersonating(active: boolean): void {
    withClient((posthog) => {
      if (active) {
        posthog.opt_out_capturing();
      } else {
        posthog.opt_in_capturing();
      }
    });
  }

  /** True once PostHog has loaded and initialised. */
  function isInitialized(): boolean {
    return client !== null;
  }

  return {
    initAnalytics,
    resetAnalytics,
    trackEvent,
    identify,
    group,
    setImpersonating,
    isInitialized,
  };
}
