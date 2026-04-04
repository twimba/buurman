# API Layer

This directory contains the Axios client and manual API modules.

**New endpoints:** Define in `openapi/src/paths/`, run `make bundle-openapi && yarn generate:api`,
then use the generated functions from `src/generated/api/` in your hooks.

**Existing manual modules:** Being migrated to generated clients. Do not add new manual API functions.

See `frontend/docs/API_STRATEGY.md` for the full migration plan.
