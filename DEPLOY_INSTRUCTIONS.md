# Deployment Instructions

## BUUR-20: Impersonation Step-Up Authentication Redesign

**Date**: 2026-03-15
**Branch**: `claude/buur-20-impersonate-user`

### Context

Replaced the redirect-based Keycloak re-authentication flow with an in-modal password confirmation dialog. The backend now validates admin passwords via Keycloak's Resource Owner Password Credentials (ROPC) grant (`grant_type=password`). This requires "Direct Access Grants" to be enabled on the `buurman-backoffice-web` Keycloak client.

### Required: Enable Direct Access Grants in Keycloak

The `buurman-backoffice-web` client must have **Direct Access Grants** enabled, otherwise all impersonation session creation and rejoin attempts will fail with "Invalid password" regardless of correct credentials.

**Via Keycloak Admin Console:**

1. Log in to Keycloak Admin Console
2. Select the `buurman-backoffice` realm
3. Go to **Clients** → `buurman-backoffice-web`
4. Under **Capability config** (or **Settings** depending on Keycloak version), enable **Direct access grants**
5. Click **Save**

**Via realm JSON import** (if reimporting):

The updated `keycloak/buurman-backoffice-realm.json` already has `"directAccessGrantsEnabled": true` set. If your Keycloak import strategy is `IGNORE_EXISTING`, you must either:
- Apply the change manually via the Admin Console (above), or
- Wipe the Keycloak database and reimport from scratch

### Verification

After enabling Direct Access Grants:

1. Go to backoffice → Users → select a user → click **Impersonate**
2. Fill in team, reason, and duration → click **Start in Read Mode** or **Start in Full Access**
3. Password confirmation dialog should appear
4. Enter the admin's correct password → session should be created and app opens in a new tab
5. Test with a wrong password → should show "Invalid password" in the dialog
6. Test rejoin from the Impersonation Sessions page → should also prompt for password
