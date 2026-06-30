import axios from 'axios';
import keycloak from '../config/keycloak';

// Derive API base URL from current hostname: app.X → api.X (handles workspace prefixes like w1-app → w1-api)
const apiHostname = window.location.hostname.replace(/\bapp\b/, 'api');
const apiPort = window.location.port ? `:${window.location.port}` : '';
const apiBaseUrl = `${window.location.protocol}//${apiHostname}${apiPort}`;

const client = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
  // Serialize array query params as repeated keys without `[]` brackets
  // (`tags=a&tags=b`) so Spring's `@RequestParam List<...>` binds them.
  // Axios's default `tags[]=a` does NOT bind server-side.
  paramsSerializer: { indexes: null },
});

const IMPERSONATION_STORAGE_KEY = 'buurman-impersonation';

function getImpersonationToken(): string | null {
  try {
    const stored = sessionStorage.getItem(IMPERSONATION_STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      if (parsed.active && parsed.token && parsed.expiresAt > Date.now()) {
        return parsed.token;
      }
    }
  } catch {
    // ignore
  }
  return null;
}

// Request interceptor: Prefer impersonation JWT, fall back to Keycloak token
client.interceptors.request.use(
  (config) => {
    const impersonationToken = getImpersonationToken();
    if (impersonationToken) {
      config.headers.Authorization = `Bearer ${impersonationToken}`;
    } else if (keycloak.authenticated && keycloak.token) {
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    const SUPPORTED_LANGS = [
      'en',
      'nl',
      'pt',
      'es',
      'fr',
      'de',
      'it',
      'sv',
      'fi',
      'el',
      'pl',
      'da',
      'nb',
    ];
    const storedLang = localStorage.getItem('buurman-language');
    config.headers['Accept-Language'] =
      storedLang && SUPPORTED_LANGS.includes(storedLang) ? storedLang : 'en';
    // Let Axios set the correct Content-Type for FormData (multipart/form-data with boundary)
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle 401 and token refresh
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Only attempt token refresh if the user was authenticated
      if (keycloak.authenticated) {
        try {
          // Try to refresh token (30 seconds before expiry)
          const refreshed = await keycloak.updateToken(30);
          if (refreshed && keycloak.token) {
            originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
            return client(originalRequest);
          }
        } catch (refreshError) {
          // Refresh failed, redirect to login
          keycloak.login();
          return Promise.reject(refreshError);
        }
      }
    }

    // Redirect unverified users to verification page (unless already there)
    if (
      error.response?.status === 403 &&
      error.response?.data?.error === 'EMAIL_NOT_VERIFIED' &&
      window.location.pathname !== '/verify-email'
    ) {
      window.location.href = '/verify-email';
      return Promise.reject(error);
    }

    // Surface rate-limit errors with specific guidance
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers?.['retry-after'];
      const message = retryAfter
        ? `Too many requests. Please wait ${retryAfter} seconds before retrying.`
        : 'Too many requests. Please wait a moment before retrying.';
      error.message = message;
    }

    return Promise.reject(error);
  }
);

export default client;
