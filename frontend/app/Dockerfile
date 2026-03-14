FROM --platform=$BUILDPLATFORM node:25.8.1-alpine3.22 AS build
WORKDIR /monorepo/frontend

ARG GIT_COMMIT=""
ARG GIT_COMMIT_SHORT=""
ARG GIT_BRANCH=""

RUN npm install -g --force corepack && corepack enable

# Copy workspace files
COPY frontend/package.json frontend/.yarnrc.yml ./
COPY frontend/packages/ui/package.json packages/ui/
COPY frontend/app/package.json app/

RUN yarn config set nodeLinker node-modules && yarn install

COPY frontend/packages/ui/ packages/ui/
COPY frontend/scripts/ scripts/
COPY frontend/app/ app/
COPY frontend/orval.config.ts ./
COPY openapi/ /monorepo/openapi/

# Generate TypeScript API clients from OpenAPI specs, then build
WORKDIR /monorepo/frontend
RUN npx orval --project app
WORKDIR /monorepo/frontend/app
RUN rm -rf dist
RUN GIT_COMMIT="$GIT_COMMIT" GIT_COMMIT_SHORT="$GIT_COMMIT_SHORT" GIT_BRANCH="$GIT_BRANCH" yarn build

FROM nginx:1.29.6-alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /monorepo/frontend/app/dist .
COPY frontend/app/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/app/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:80 || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
