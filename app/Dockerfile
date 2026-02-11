FROM node:24.13.0-alpine3.22 AS build
WORKDIR /monorepo

ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
ARG VITE_GOOGLE_MAPS_API_KEY

ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
ENV VITE_GOOGLE_MAPS_API_KEY=$VITE_GOOGLE_MAPS_API_KEY

RUN corepack enable

# Copy workspace files
COPY package.json .yarnrc.yml ./
COPY packages/ui/package.json packages/ui/
COPY app/package.json app/

RUN yarn config set nodeLinker node-modules && yarn install

COPY packages/ui/ packages/ui/
COPY app/ app/

WORKDIR /monorepo/app
RUN rm -rf dist
RUN yarn build

FROM nginx:1.29.4-alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /monorepo/app/dist .
COPY app/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
