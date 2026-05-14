import { useCallback, useRef } from 'react';

/**
 * Google Identity Services token-client integration. On demand, opens a popup that asks the user
 * to consent to the `drive.file` scope and returns a short-lived OAuth 2.0 access token. The
 * token is never persisted — the caller passes it straight to the backend for a single export.
 *
 * The GIS script is loaded lazily on first use; failed loads are not cached, so a subsequent call
 * retries the load (e.g. after the user comes back online).
 */

const GIS_SCRIPT_URL = 'https://accounts.google.com/gsi/client';
const GIS_SCOPES = 'https://www.googleapis.com/auth/drive.file';

interface GisTokenResponse {
  access_token?: string;
  error?: string;
  error_description?: string;
}

interface GisTokenClient {
  callback: (response: GisTokenResponse) => void;
  error_callback?: (error: { type: string; message?: string }) => void;
  requestAccessToken: (overrideConfig?: { prompt?: string }) => void;
}

interface GisGlobal {
  accounts?: {
    oauth2?: {
      initTokenClient: (config: {
        client_id: string;
        scope: string;
        callback: (response: GisTokenResponse) => void;
        error_callback?: (error: { type: string; message?: string }) => void;
      }) => GisTokenClient;
    };
  };
}

declare global {
  interface Window {
    google?: GisGlobal;
  }
}

let gisLoadPromise: Promise<void> | undefined;

function loadGis(): Promise<void> {
  if (gisLoadPromise) {
    return gisLoadPromise;
  }
  const attempt = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${GIS_SCRIPT_URL}"]`
    );
    if (existing) {
      if (window.google?.accounts?.oauth2) {
        resolve();
      } else {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener(
          'error',
          () => reject(new Error('Failed to load Google Identity Services')),
          { once: true }
        );
      }
      return;
    }
    const script = document.createElement('script');
    script.src = GIS_SCRIPT_URL;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error('Failed to load Google Identity Services'));
    document.head.appendChild(script);
  });
  // Clear the cached promise on failure so subsequent calls retry the load.
  attempt.catch(() => {
    if (gisLoadPromise === attempt) {
      gisLoadPromise = undefined;
    }
  });
  gisLoadPromise = attempt;
  return attempt;
}

export class GoogleAccessTokenError extends Error {
  readonly type: string;
  constructor(type: string, message: string) {
    super(message);
    this.type = type;
  }
}

/**
 * Hook that returns an async function: when invoked, it opens the Google consent popup and
 * resolves to a short-lived access token (drive.file scope). Rejects on cancellation, popup
 * blockers, or any auth error.
 */
export function useGoogleAccessToken(): () => Promise<string> {
  const clientRef = useRef<GisTokenClient | undefined>(undefined);

  return useCallback(async () => {
    const clientId = import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID;
    if (!clientId) {
      throw new GoogleAccessTokenError(
        'misconfigured',
        'Google Sheets export is not configured for this environment'
      );
    }

    await loadGis();

    const oauth2 = window.google?.accounts?.oauth2;
    if (!oauth2) {
      throw new GoogleAccessTokenError(
        'gis_unavailable',
        'Google Identity Services failed to load'
      );
    }

    return new Promise<string>((resolve, reject) => {
      const tokenClient = oauth2.initTokenClient({
        client_id: clientId,
        scope: GIS_SCOPES,
        callback: (response) => {
          if (response.access_token) {
            resolve(response.access_token);
            return;
          }
          reject(
            new GoogleAccessTokenError(
              response.error ?? 'no_token',
              response.error_description ??
                'Google did not return an access token'
            )
          );
        },
        error_callback: (error) => {
          reject(
            new GoogleAccessTokenError(
              error.type,
              error.message ??
                (error.type === 'popup_closed'
                  ? 'Sign-in was cancelled'
                  : 'Google sign-in failed')
            )
          );
        },
      });
      clientRef.current = tokenClient;
      tokenClient.requestAccessToken({ prompt: '' });
    });
  }, []);
}
