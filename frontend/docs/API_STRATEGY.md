# API Strategy: Orval-Generated Clients

## Decision

Orval-generated API clients from the OpenAPI spec are the **source of truth** for all frontend API interactions. The manual `app/src/api/*.ts` modules and `app/src/types/*.ts` interfaces that duplicate OpenAPI schemas will be retired over time in favor of generated code.

## Current State

The frontend currently has a dual-layer API setup:

| Layer | Location | Status |
|-------|----------|--------|
| Manual API modules | `app/src/api/*.ts` (30 files) | Legacy, being migrated |
| Manual type definitions | `app/src/types/*.ts` (16 files) | Legacy, being replaced by generated models |
| Orval config | `orval.config.ts` | Active, generates into `app/src/generated/` |
| Axios client singleton | `app/src/api/client.ts` | Stays manual (permanent) |
| Orval bridge | `app/src/api/orval-client.ts` | Stays manual (permanent) |

### How the layers connect

1. **`client.ts`** creates an Axios instance with the Keycloak auth interceptor, token refresh, impersonation support, and error handling.
2. **`orval-client.ts`** exports a `customInstance` function that wraps `client.ts` so Orval-generated functions use the same Axios instance (and therefore the same auth/interceptor logic).
3. **Orval** generates typed API functions (one file per OpenAPI tag) and TypeScript models (one file per schema), all wired through `customInstance`.

### Orval configuration summary

- **Input**: `../openapi/app.yaml` (bundled OpenAPI spec)
- **Output**: `app/src/generated/api/` (API functions) and `app/src/generated/models/` (TypeScript types)
- **Client style**: `axios-functions` (standalone functions, not classes)
- **Split mode**: `tags-split` (one file per OpenAPI tag)
- **Mutator**: `app/src/api/orval-client.ts` (`customInstance`)
- **Clean on generate**: `true` (output directories are wiped and regenerated each run)

A parallel config exists for the backoffice app (`backoffice/src/generated/`).

## New Feature Workflow

When adding a new API endpoint:

1. Define the endpoint in `openapi/src/paths/<domain>.yaml`
2. Define request/response schemas in `openapi/src/app.yaml` (under `components/schemas`)
3. Bundle the spec: `make bundle-openapi`
4. Generate clients: `yarn generate:api` (or just `cd frontend && yarn generate:api`)
5. Use the generated function directly in your React Query hook -- import from `src/generated/api/<tag>`
6. Use generated types from `src/generated/models/` for any TypeScript needs

Do **not** add new functions to the manual `app/src/api/*.ts` modules.

## Migration Plan

Manual API modules will be replaced **one domain at a time**. Each migration PR should:

1. Verify the OpenAPI spec covers all endpoints for that domain (add any missing ones)
2. Run `make bundle-openapi && yarn generate:api`
3. Update React Query hooks to import from `src/generated/api/` instead of `src/api/<domain>.ts`
4. Update component imports to use generated model types from `src/generated/models/`
5. Remove the manual API module (`src/api/<domain>.ts`)
6. Remove the manual type file (`src/types/<domain>.ts`) if all its types are now generated
7. Verify the app builds and all affected pages work

### Migration order (suggested, not mandatory)

Prioritize domains with the simplest API surface first to build confidence:

- `health`, `featureFlags`, `reference` -- tiny, low risk
- `contacts`, `properties`, `contracts` -- core CRUD, high usage
- `payments`, `expenses` -- financial domain
- Remaining modules

### What stays manual (permanent)

| File | Reason |
|------|--------|
| `app/src/api/client.ts` | Axios singleton with auth interceptors, token refresh, impersonation, error handling. This is infrastructure, not API surface. |
| `app/src/api/orval-client.ts` | Bridge between Orval-generated code and the Axios client. Required by Orval's mutator config. |

### What gets generated (replace manual code)

| Artifact | Source |
|----------|--------|
| API functions (GET, POST, PUT, DELETE wrappers) | Orval from OpenAPI paths |
| Request/response TypeScript types | Orval from OpenAPI schemas |

### What stays in `types/*.ts` (frontend-only)

Some types in `app/src/types/` are **not** API schemas -- they exist only for frontend concerns. These should remain manual:

- Form state types (e.g., intermediate form shapes before submission)
- Label/display maps (e.g., enum-to-human-readable-string mappings)
- UI-specific enums or union types not in the OpenAPI spec
- Pagination helpers (`PageResponse`, `PageParams` in `common.ts`) -- until these are in the spec

If a type file contains a mix of API types and frontend-only types, split out the frontend-only types and delete the rest once generated models cover them.

## Commands Reference

| Command | What it does |
|---------|--------------|
| `make bundle-openapi` | Bundles `openapi/src/` into `openapi/app.yaml` |
| `yarn generate:api` | Runs Orval to generate clients + models from bundled spec |
| `yarn build` | Runs Orval first, then builds both app and backoffice |
