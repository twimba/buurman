import { defineConfig } from 'orval';

export default defineConfig({
  app: {
    input: { target: '../openapi/app.yaml' },
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
    hooks: {
      // Generated identifier types (UnitIdentifier, PropertyIdentifier, ...) are structural
      // `string` aliases, so passing one identifier type where another is expected compiles
      // silently and only fails at runtime (BUUR-106: the WWS pre-fill hook did this for months).
      // Rebrand the couple of identifier types WWS actually mixes up so a mismatch is a compile
      // error instead. Runs after every generation since `clean: true` wipes the output first.
      afterAllFilesWrite: 'node ./scripts/brand-identifiers.mjs',
    },
  },
  backoffice: {
    input: { target: '../openapi/backoffice.yaml' },
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
    hooks: {
      afterAllFilesWrite: 'node ./scripts/brand-identifiers.mjs',
    },
  },
});
