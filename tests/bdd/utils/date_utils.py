"""Date utilities for BDD tests.

Provides relative date resolution so feature files use human-readable
expressions like "next month" or "in 30 days" instead of hardcoded dates
that would break after a fixed calendar date.
"""

from __future__ import annotations

import re
from datetime import date, timedelta
from typing import Optional

# Pattern → resolver lookup.  Order matters: more specific patterns first.
_RELATIVE_PATTERNS: list[tuple[re.Pattern, object]] = [
    (re.compile(r"^today$", re.IGNORECASE), lambda: date.today()),
    (re.compile(r"^yesterday$", re.IGNORECASE), lambda: date.today() - timedelta(days=1)),
    (re.compile(r"^tomorrow$", re.IGNORECASE), lambda: date.today() + timedelta(days=1)),
    (re.compile(r"^in (\d+) days?$", re.IGNORECASE), None),
    (re.compile(r"^(\d+) days? ago$", re.IGNORECASE), None),
    (re.compile(r"^in (\d+) months?$", re.IGNORECASE), None),
    (re.compile(r"^(\d+) months? ago$", re.IGNORECASE), None),
    (re.compile(r"^next month$", re.IGNORECASE), None),
    (re.compile(r"^last month$", re.IGNORECASE), None),
]


def _add_months(d: date, months: int) -> date:
    """Add (or subtract) calendar months, clamping to valid day-of-month."""
    month = d.month - 1 + months
    year = d.year + month // 12
    month = month % 12 + 1
    # Clamp day to the last valid day of the target month
    import calendar

    max_day = calendar.monthrange(year, month)[1]
    return date(year, month, min(d.day, max_day))


def resolve_date(value: str) -> str:
    """Resolve a date expression to an ISO date string (YYYY-MM-DD).

    Accepts:
      - ISO dates passthrough:  "2026-04-01" → "2026-04-01"
      - Relative expressions:   "today", "tomorrow", "yesterday"
      - Day offsets:             "in 30 days", "3 days ago"
      - Month offsets:           "in 2 months", "1 month ago",
                                 "next month", "last month"

    Returns:
        ISO-formatted date string.

    Raises:
        ValueError: If the expression cannot be parsed.
    """
    value = value.strip()

    # Passthrough: already an ISO date
    if re.match(r"^\d{4}-\d{2}-\d{2}$", value):
        return value

    today = date.today()

    # Simple keywords
    lower = value.lower()
    if lower == "today":
        return today.isoformat()
    if lower == "tomorrow":
        return (today + timedelta(days=1)).isoformat()
    if lower == "yesterday":
        return (today - timedelta(days=1)).isoformat()
    if lower == "next month":
        return _add_months(today, 1).isoformat()
    if lower == "last month":
        return _add_months(today, -1).isoformat()

    # "in N days"
    m = re.match(r"^in (\d+) days?$", value, re.IGNORECASE)
    if m:
        return (today + timedelta(days=int(m.group(1)))).isoformat()

    # "N days ago"
    m = re.match(r"^(\d+) days? ago$", value, re.IGNORECASE)
    if m:
        return (today - timedelta(days=int(m.group(1)))).isoformat()

    # "in N months"
    m = re.match(r"^in (\d+) months?$", value, re.IGNORECASE)
    if m:
        return _add_months(today, int(m.group(1))).isoformat()

    # "N months ago"
    m = re.match(r"^(\d+) months? ago$", value, re.IGNORECASE)
    if m:
        return _add_months(today, -int(m.group(1))).isoformat()

    msg = f"Cannot resolve date expression: '{value}'"
    raise ValueError(msg)


def resolve_datetime(value: str) -> str:
    """Resolve a date expression to an ISO datetime string.

    Same as resolve_date() but appends T00:00:00Z for use with
    date-time fields in the API.
    """
    iso_date = resolve_date(value)
    return f"{iso_date}T00:00:00Z"
