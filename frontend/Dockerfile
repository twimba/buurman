FROM --platform=$BUILDPLATFORM node:25.8.1-alpine3.22 AS build

# TARGET_APP: "app" or "backoffice"
ARG TARGET_APP
ARG GIT_COMMIT=""
ARG GIT_COMMIT_SHORT=""
ARG GIT_BRANCH=""

WORKDIR /monorepo/frontend

RUN npm install -g --force corepack && corepack enable

# Copy ALL workspace package.json files so Yarn resolves the full dependency
# graph identically to local installs (prevents duplicate transitive deps).
COPY frontend/package.json frontend/.yarnrc.yml frontend/yarn.lock ./
COPY frontend/packages/ui/package.json packages/ui/
COPY frontend/app/package.json app/
COPY frontend/backoffice/package.json backoffice/

RUN yarn install --frozen-lockfile

# Copy shared UI library + build scripts
COPY frontend/packages/ui/ packages/ui/
COPY frontend/scripts/ scripts/

# Copy target app source + OpenAPI spec for code generation
COPY frontend/${TARGET_APP}/ ${TARGET_APP}/
COPY frontend/orval.config.ts ./
COPY openapi/ /monorepo/openapi/

# Generate TypeScript API clients from OpenAPI spec, then build
RUN npx orval --project ${TARGET_APP}

WORKDIR /monorepo/frontend/${TARGET_APP}
RUN GIT_COMMIT="$GIT_COMMIT" GIT_COMMIT_SHORT="$GIT_COMMIT_SHORT" GIT_BRANCH="$GIT_BRANCH" yarn build

# --- Production image ---
FROM nginx:1.29.6-alpine

ARG TARGET_APP

WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /monorepo/frontend/${TARGET_APP}/dist .
COPY frontend/${TARGET_APP}/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/${TARGET_APP}/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:80 || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
