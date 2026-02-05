# Build stage
FROM node:24.13.0-alpine3.22 AS build
WORKDIR /app

# Build arguments for environment variables
ARG VITE_API_URL
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID

# Set as environment variables for Vite build
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

# Enable Corepack for Yarn 4 (required by packageManager field)
RUN corepack enable

# Copy package files and install dependencies (cached layer)
COPY package.json yarn.lock ./
RUN yarn config set nodeLinker node-modules && yarn install

# Copy source and build
COPY . .
RUN rm -rf dist
RUN yarn build

# Runtime stage - Nginx to serve static files
FROM nginx:1.29.4-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built app from build stage
COPY --from=build /app/dist .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
