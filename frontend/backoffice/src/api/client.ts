import axios from 'axios';
import keycloak from '../config/keycloak';

// Derive API base URL from current hostname: backoffice.X → api.X/backoffice (handles workspace prefixes like w1-backoffice → w1-api)
const apiHostname = window.location.hostname.replace(/\bbackoffice\b/, 'api');
const apiPort = window.location.port ? `:${window.location.port}` : '';
const apiBaseUrl = `${window.location.protocol}//${apiHostname}${apiPort}/backoffice`;

const client = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
  // Serialize array query params as repeated keys without `[]` brackets
  // (`group=a&group=b`) so Spring's `@RequestParam List<...>` binds them.
  paramsSerializer: { indexes: null },
});

// Request interceptor: Attach JWT token
client.interceptors.request.use(
  (config) => {
    if (keycloak.authenticated && keycloak.token) {
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
          const refreshed = await keycloak.updateToken(30);
          if (refreshed && keycloak.token) {
            originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
            return client(originalRequest);
          }
        } catch (refreshError) {
          keycloak.login();
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);

export default client;
