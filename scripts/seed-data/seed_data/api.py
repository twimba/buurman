"""Thin authenticated REST client for the Buurman API.

One method per endpoint the seeder uses. Each call retries on 429/5xx with
exponential backoff. Endpoints live at the root (no /api prefix), matching the
BDD test client.
"""

from __future__ import annotations

import time

import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

_RETRY_STATUS = {429, 500, 502, 503, 504}


class ApiError(RuntimeError):
    pass


class BuurmanApi:
    def __init__(self, base_url: str, token: str, *, max_retries: int = 5) -> None:
        self.base_url = base_url.rstrip("/")
        self.max_retries = max_retries
        self.session = requests.Session()
        self.session.verify = False
        self.session.headers.update(
            {
                "Authorization": f"Bearer {token}",
                "Accept": "application/json",
            }
        )

    def _request(self, method: str, path: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{path}"
        last = None
        for attempt in range(self.max_retries):
            resp = self.session.request(method, url, timeout=60, **kwargs)
            if resp.status_code not in _RETRY_STATUS:
                if resp.status_code >= 400:
                    raise ApiError(f"{method} {path} -> {resp.status_code}: {resp.text[:300]}")
                return resp
            last = resp
            time.sleep(min(2 ** attempt, 10))
        raise ApiError(
            f"{method} {path} -> {last.status_code} after {self.max_retries} retries"
        )

    def _post_json(self, path: str, body: dict) -> dict:
        resp = self._request(
            "POST", path, json=body, headers={"Content-Type": "application/json"}
        )
        return resp.json()

    def _upload(self, path: str, *, filename: str, content: bytes, mime: str, title: str) -> dict:
        files = {"file": (filename, content, mime)}
        resp = self._request("POST", path, files=files, params={"title": title})
        return resp.json()

    # --- entity creation -------------------------------------------------

    def create_property(self, body: dict) -> str:
        return self._post_json("/properties", body)["identifier"]

    def create_contact(self, body: dict) -> str:
        return self._post_json("/contacts", body)["identifier"]

    def create_contract(self, body: dict) -> str:
        return self._post_json("/contracts", body)["identifier"]

    def change_contract_status(self, contract_id: str, status: str) -> None:
        # DRAFT -> ACTIVE, then ACTIVE -> EXPIRED/TERMINATED. One active per property.
        self._post_json(f"/contracts/{contract_id}/change-status", {"status": status})

    def create_payment(self, body: dict) -> str:
        return self._post_json("/payments", body)["identifier"]

    # --- attachments -----------------------------------------------------

    def upload_property_photo(self, prop_id: str, **kw) -> str:
        return self._upload(f"/properties/{prop_id}/photos", **kw)["identifier"]

    def upload_property_document(self, prop_id: str, **kw) -> str:
        return self._upload(f"/properties/{prop_id}/documents", **kw)["identifier"]

    def upload_contact_document(self, contact_id: str, **kw) -> str:
        return self._upload(f"/contacts/{contact_id}/documents", **kw)["identifier"]

    def upload_contract_document(self, contract_id: str, **kw) -> str:
        return self._upload(f"/contracts/{contract_id}/documents", **kw)["identifier"]

    def upload_payment_document(self, payment_id: str, **kw) -> str:
        return self._upload(f"/payments/{payment_id}/documents", **kw)["identifier"]
