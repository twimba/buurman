import axios from 'axios';
import keycloak from '../config/keycloak';

// Derive API base URL from current hostname: app.X → api.X
const apiBaseUrl = `${window.location.protocol}//api.${window.location.hostname.replace(/^app\./, '')}`;

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
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

    return Promise.reject(error);
  }
);

export default client;
