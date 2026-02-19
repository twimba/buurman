FROM --platform=$BUILDPLATFORM node:24.13.0-alpine3.22 AS build
WORKDIR /monorepo

RUN corepack enable

# Copy workspace files
COPY package.json .yarnrc.yml ./
COPY packages/ui/package.json packages/ui/
COPY app/package.json app/

RUN yarn config set nodeLinker node-modules && yarn install

COPY packages/ui/ packages/ui/
COPY scripts/ scripts/
COPY app/ app/

WORKDIR /monorepo/app
RUN rm -rf dist
RUN yarn build

FROM nginx:1.29.4-alpine
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
