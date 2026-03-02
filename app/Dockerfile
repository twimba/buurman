FROM --platform=$BUILDPLATFORM node:25.7.0-alpine3.22 AS build
WORKDIR /monorepo

ARG GIT_COMMIT=""
ARG GIT_COMMIT_SHORT=""
ARG GIT_BRANCH=""

RUN corepack enable

# Copy workspace files
COPY package.json .yarnrc.yml ./
COPY packages/ui/package.json packages/ui/
COPY app/package.json app/

RUN yarn config set nodeLinker node-modules && yarn install

COPY packages/ui/ packages/ui/
COPY scripts/ scripts/
COPY app/ app/
COPY orval.config.ts ./
COPY openapi/ openapi/

# Generate TypeScript API clients from OpenAPI specs, then build
WORKDIR /monorepo
RUN yarn generate:api
WORKDIR /monorepo/app
RUN rm -rf dist
RUN GIT_COMMIT="$GIT_COMMIT" GIT_COMMIT_SHORT="$GIT_COMMIT_SHORT" GIT_BRANCH="$GIT_BRANCH" yarn build

FROM nginx:1.29.5-alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /monorepo/app/dist .
COPY app/nginx.conf /etc/nginx/conf.d/default.conf
COPY app/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:80 || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
