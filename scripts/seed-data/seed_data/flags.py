"""Manage the global `invitation_required` feature flag via the backoffice API.

Registration (/auth/register) is blocked while the global `invitation_required`
flag is on. We flip it off for the duration of the run and restore the original
value afterwards. This goes through the backoffice admin API (separate Keycloak
realm `buurman-backoffice`), which also evicts the backend flag cache — a raw DB
UPDATE would not.
"""

from __future__ import annotations

from contextlib import contextmanager

import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

BACKOFFICE_REALM = "buurman-backoffice"
BACKOFFICE_CLIENT_ID = "buurman-backoffice-web"
INVITATION_REQUIRED = "invitation_required"


class FlagAdminError(RuntimeError):
    pass


def _token(keycloak_url: str, client_id: str, username: str, password: str) -> str:
    resp = requests.post(
        f"{keycloak_url}/realms/{BACKOFFICE_REALM}/protocol/openid-connect/token",
        data={
            "grant_type": "password",
            "client_id": client_id,
            "username": username,
            "password": password,
            "scope": "openid email profile",
        },
        verify=False,
        timeout=30,
    )
    if resp.status_code >= 400:
        raise FlagAdminError(
            f"backoffice login as {username} -> {resp.status_code}: {resp.text[:200]}"
        )
    return resp.json()["access_token"]


class BackofficeFlags:
    """Thin client for reading/updating global feature flags."""

    def __init__(self, api_url: str, keycloak_url: str, username: str, password: str,
                 client_id: str = BACKOFFICE_CLIENT_ID) -> None:
        self.base = f"{api_url.rstrip('/')}/backoffice"
        token = _token(keycloak_url, client_id, username, password)
        self.session = requests.Session()
        self.session.verify = False
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def get_enabled(self, flag: str) -> bool:
        resp = self.session.get(f"{self.base}/feature-flags", timeout=30)
        resp.raise_for_status()
        entry = resp.json().get(flag)
        if not isinstance(entry, dict) or "enabled" not in entry:
            raise FlagAdminError(f"flag '{flag}' not found in global flags")
        return bool(entry["enabled"])

    def set_enabled(self, flag: str, enabled: bool) -> None:
        resp = self.session.patch(
            f"{self.base}/feature-flags/{flag}",
            json={"enabled": enabled},
            headers={"Content-Type": "application/json"},
            timeout=30,
        )
        if resp.status_code >= 400:
            raise FlagAdminError(
                f"PATCH feature-flags/{flag} -> {resp.status_code}: {resp.text[:200]}"
            )


@contextmanager
def invitation_required_disabled(
    *, api_url: str, keycloak_url: str, username: str, password: str, client_id: str
):
    """Disable invitation_required for the run, restoring its original value after."""
    client = BackofficeFlags(api_url, keycloak_url, username, password, client_id)
    original = client.get_enabled(INVITATION_REQUIRED)
    if original:
        print(f"Disabling global flag '{INVITATION_REQUIRED}' (was enabled) ...")
        client.set_enabled(INVITATION_REQUIRED, False)
    else:
        print(f"Global flag '{INVITATION_REQUIRED}' already disabled — leaving as-is.")
    try:
        yield
    finally:
        if original:
            print(f"Restoring global flag '{INVITATION_REQUIRED}' to enabled ...")
            try:
                client.set_enabled(INVITATION_REQUIRED, True)
            except Exception as exc:  # noqa: BLE001
                print(f"  ! failed to restore '{INVITATION_REQUIRED}': {str(exc)[:160]}")
                print("    Re-enable it manually in Backoffice -> Feature Flags.")
