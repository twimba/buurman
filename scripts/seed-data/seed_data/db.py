"""Lightweight DB helper for inline operations during the run.

Currently only marks accounts as email-verified. The API path for that
(/auth/verify-email) is IP-rate-limited and cannot keep up with bulk seeding,
so — like the backdating pass — we set the timestamp directly. The backend reads
`users.email_verified_at` from the DB on every request (JwtAuthenticationConverter),
so a created row immediately passes the EmailVerificationFilter.

Connections are thread-local (one per worker thread, reused across that thread's users).
"""

from __future__ import annotations

import threading
from datetime import datetime

import psycopg


class DbClient:
    def __init__(self, db_url: str) -> None:
        self.db_url = db_url
        self._local = threading.local()
        self._conns: list[psycopg.Connection] = []
        self._lock = threading.Lock()

    def _conn(self) -> psycopg.Connection:
        conn = getattr(self._local, "conn", None)
        if conn is None or conn.closed:
            conn = psycopg.connect(self.db_url)
            self._local.conn = conn
            with self._lock:
                self._conns.append(conn)
        return conn

    def check(self) -> None:
        """Fail fast if the DB is unreachable."""
        with psycopg.connect(self.db_url, connect_timeout=5) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")

    def mark_email_verified(self, user_identifier: str, dt: datetime) -> None:
        conn = self._conn()
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE users SET email_verified_at = %s WHERE identifier = %s",
                (dt, user_identifier),
            )
        conn.commit()

    def close(self) -> None:
        with self._lock:
            for conn in self._conns:
                try:
                    conn.close()
                except Exception:  # noqa: BLE001
                    pass
