"""The only direct-DB module: backdate historical timestamps the API can't set.

Every row is created through the API (which stamps created_at = now). To make the
simulated N-months history real in the dashboard's time-series, we run one batched
pass of UPDATEs keyed by the entity's `identifier` (Sid) returned by the API.

Columns confirmed from Flyway migrations:
  teams/users                      created_at, updated_at   (match: identifier)
  team_members                     invited_at, joined_at    (match: team identifier via join)
  properties/contacts/contracts/payments  created_at, updated_at  (match: identifier)
  documents/photos                 uploaded_at              (match: identifier)
"""

from __future__ import annotations

import threading
from collections import defaultdict
from datetime import datetime

import psycopg

# table -> SQL with positional params. created/updated tables share one shape.
_CREATED_TABLES = ("properties", "contacts", "contracts", "payments")
_UPLOADED_TABLES = ("documents", "photos")


class Collector:
    """Thread-safe buffer of (identifier, timestamp) backdate records."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.teams: list[tuple[str, datetime]] = []
        self.users: list[tuple[str, datetime]] = []
        self.team_members: list[tuple[str, datetime]] = []  # (team_identifier, dt)
        self.rows: dict[str, list[tuple[str, datetime]]] = defaultdict(list)

    def add_account(self, team_identifier: str, user_identifier: str, dt: datetime) -> None:
        with self._lock:
            self.teams.append((team_identifier, dt))
            self.users.append((user_identifier, dt))
            self.team_members.append((team_identifier, dt))

    def add(self, table: str, identifier: str, dt: datetime) -> None:
        with self._lock:
            self.rows[table].append((identifier, dt))

    def total(self) -> int:
        return (
            len(self.teams) + len(self.users) + len(self.team_members)
            + sum(len(v) for v in self.rows.values())
        )


def apply(db_url: str, collector: Collector, batch: int = 1000) -> int:
    """Apply all buffered backdates. Returns the number of rows updated."""
    updated = 0
    with psycopg.connect(db_url) as conn:
        with conn.cursor() as cur:
            updated += _run(
                cur,
                "UPDATE teams SET created_at = %s, updated_at = %s WHERE identifier = %s",
                [(dt, dt, ident) for ident, dt in collector.teams],
                batch,
            )
            updated += _run(
                cur,
                "UPDATE users SET created_at = %s, updated_at = %s WHERE identifier = %s",
                [(dt, dt, ident) for ident, dt in collector.users],
                batch,
            )
            updated += _run(
                cur,
                "UPDATE team_members tm SET invited_at = %s, joined_at = %s "
                "FROM teams t WHERE tm.team_id = t.id AND t.identifier = %s",
                [(dt, dt, ident) for ident, dt in collector.team_members],
                batch,
            )
            for table in _CREATED_TABLES:
                _run(
                    cur,
                    f"UPDATE {table} SET created_at = %s, updated_at = %s WHERE identifier = %s",
                    [(dt, dt, ident) for ident, dt in collector.rows.get(table, [])],
                    batch,
                )
                updated += len(collector.rows.get(table, []))
            for table in _UPLOADED_TABLES:
                _run(
                    cur,
                    f"UPDATE {table} SET uploaded_at = %s WHERE identifier = %s",
                    [(dt, ident) for ident, dt in collector.rows.get(table, [])],
                    batch,
                )
                updated += len(collector.rows.get(table, []))
        conn.commit()
    return updated


def _run(cur, sql: str, params: list[tuple], batch: int) -> int:
    for i in range(0, len(params), batch):
        cur.executemany(sql, params[i : i + batch])
    return len(params)
