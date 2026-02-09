import axios from 'axios';
import keycloak from '../config/keycloak';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
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
