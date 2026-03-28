"""Thin HTTP client wrapper for Buurman API.

All BDD test interactions with the system go through this client.
No production code is imported — only raw HTTP via requests.
"""

from __future__ import annotations

import requests
import urllib3

# Suppress InsecureRequestWarning for local dev TLS certs
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class BuurmanApiClient:
    """HTTP client for the Buurman REST API.

    Wraps requests.Session with auth header management and base URL resolution.
    All methods return raw requests.Response objects — assertions happen in step definitions.
    """

    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.verify = False  # Local dev uses self-signed certs
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json",
        })

    def set_auth(self, token: str) -> None:
        """Set the Bearer token for subsequent requests."""
        self.session.headers["Authorization"] = f"Bearer {token}"

    def clear_auth(self) -> None:
        """Remove the auth header."""
        self.session.headers.pop("Authorization", None)

    def get(self, path: str, **kwargs) -> requests.Response:
        return self.session.get(f"{self.base_url}{path}", **kwargs)

    def post(self, path: str, **kwargs) -> requests.Response:
        return self.session.post(f"{self.base_url}{path}", **kwargs)

    def put(self, path: str, **kwargs) -> requests.Response:
        return self.session.put(f"{self.base_url}{path}", **kwargs)

    def delete(self, path: str, **kwargs) -> requests.Response:
        return self.session.delete(f"{self.base_url}{path}", **kwargs)

    def post_multipart(self, path: str, files: dict, **kwargs) -> requests.Response:
        """POST with multipart/form-data (for file uploads).

        Temporarily removes Content-Type header so requests sets the boundary.
        """
        headers = {k: v for k, v in self.session.headers.items() if k != "Content-Type"}
        return self.session.post(
            f"{self.base_url}{path}",
            files=files,
            headers=headers,
            **kwargs,
        )
