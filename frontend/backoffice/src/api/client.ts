import axios from "axios";
import keycloak from "../config/keycloak";

// Derive API base URL from current hostname: backoffice.X → api.X/backoffice
const apiBaseUrl = `${window.location.protocol}//api.${window.location.hostname.replace(/^backoffice\./, "")}/backoffice`;

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    "Content-Type": "application/json",
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
  (error) => Promise.reject(error),
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
  },
);

export default client;
