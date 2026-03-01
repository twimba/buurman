import { defineConfig } from 'orval';

export default defineConfig({
  app: {
    input: { target: './openapi/app.yaml' },
    output: {
      target: './app/src/generated/api',
      schemas: './app/src/generated/models',
      client: 'axios-functions',
      mode: 'tags-split',
      prettier: true,
      clean: true,
      override: {
        mutator: {
          path: './app/src/api/orval-client.ts',
          name: 'customInstance',
        },
      },
    },
  },
  backoffice: {
    input: { target: './openapi/backoffice.yaml' },
    output: {
      target: './backoffice/src/generated/api',
      schemas: './backoffice/src/generated/models',
      client: 'axios-functions',
      mode: 'tags-split',
      prettier: true,
      clean: true,
      override: {
        mutator: {
          path: './backoffice/src/api/orval-client.ts',
          name: 'customInstance',
        },
      },
    },
  },
});
