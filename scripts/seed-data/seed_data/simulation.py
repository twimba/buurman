"""Timeline simulation: signup growth curve, per-user portfolios, payment history.

Produces a list of plain-data UserPlan objects describing exactly what to create.
The runner executes them via the API; the backdater uses the embedded timestamps.
Everything is deterministic given Config.seed.
"""

from __future__ import annotations

import math
import random
from dataclasses import dataclass, field
from datetime import datetime, timedelta

from dateutil.relativedelta import relativedelta

from .config import Config
from .generators import pick_country


@dataclass
class PaymentPlan:
    amount: int
    due_dt: datetime
    paid: bool
    payment_dt: datetime | None


@dataclass
class LeasePlan:
    contract_type: str
    start_dt: datetime
    end_dt: datetime | None
    signed_dt: datetime
    rent: int
    extra_role: str | None  # GUARANTOR / EXTRA_TENANT, or None
    payments: list[PaymentPlan] = field(default_factory=list)


@dataclass
class PropertyPlan:
    created_dt: datetime
    status: str
    leases: list[LeasePlan] = field(default_factory=list)


@dataclass
class UserPlan:
    index: int
    seed: int
    signup_dt: datetime
    country: str
    locale: str
    phone_cc: str
    currency: str
    org_contacts: int
    properties: list[PropertyPlan] = field(default_factory=list)


def _clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def _growth_weight(growth: str, t: float) -> float:
    """Relative monthly signup intensity at normalized time t in [0, 1]."""
    if growth == "linear":
        return 0.2 + t
    if growth == "logistic":
        return 1.0 / (1.0 + math.exp(-10.0 * (t - 0.55)))
    # exponential (default): accelerating startup growth
    return math.exp(3.0 * t)


def _cohort_sizes(users: int, months: int, growth: str) -> list[int]:
    weights = [_growth_weight(growth, i / max(1, months - 1)) for i in range(months)]
    total = sum(weights)
    raw = [users * w / total for w in weights]
    counts = [int(x) for x in raw]
    remainder = users - sum(counts)
    order = sorted(range(months), key=lambda i: raw[i] - counts[i], reverse=True)
    for i in range(remainder):
        counts[order[i]] += 1
    return counts


def _rng_for(seed: int, index: int) -> random.Random:
    return random.Random(seed * 1_000_003 + index)


def _build_payments(
    start: datetime, end: datetime | None, now: datetime, rent: int, rng: random.Random
) -> list[PaymentPlan]:
    payments: list[PaymentPlan] = []
    due = start.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    if due < start.replace(hour=0, minute=0, second=0, microsecond=0):
        due += relativedelta(months=1)
    horizon = now if end is None else min(end, now)
    guard = 0
    while due <= horizon and guard < 60:
        guard += 1
        is_past = due < (now - timedelta(days=2))
        paid = False
        payment_dt = None
        if is_past:
            if rng.random() < 0.92:  # most past dues are settled
                paid = True
                payment_dt = min(now, due + timedelta(days=rng.randint(0, 6)))
            # else: left unpaid -> server marks OVERDUE
        # current-period dues stay PENDING
        payments.append(PaymentPlan(amount=rent, due_dt=due, paid=paid, payment_dt=payment_dt))
        due += relativedelta(months=1)
    return payments


def _build_property(
    signup: datetime, now: datetime, country_rent: int, cfg: Config, rng: random.Random
) -> PropertyPlan:
    span = (now - signup).total_seconds()
    frac = rng.random() ** 1.5  # acquired earlier in tenure, on average
    created = signup + timedelta(seconds=span * frac * 0.85)
    base_rent = max(300, int(country_rent * _clamp(rng.gauss(1.0, 0.25), 0.5, 2.2)))

    leases: list[LeasePlan] = []
    cursor = created + timedelta(days=rng.randint(3, 45))
    while cursor < now and len(leases) < cfg.max_leases_per_property:
        indefinite = rng.random() < 0.30
        term = rng.choice([12, 12, 24, 24, 36])
        start = cursor
        end = None if indefinite else start + relativedelta(months=term)
        signed = max(created, start - timedelta(days=rng.randint(3, 30)))
        rent = int(base_rent * (1.0 + 0.03 * len(leases)))  # small step-ups on re-let
        extra_role = None
        roll = rng.random()
        if roll < 0.18:
            extra_role = "GUARANTOR"
        elif roll < 0.30:
            extra_role = "EXTRA_TENANT"
        lease = LeasePlan(
            contract_type="INDEFINITE" if indefinite else "FIXED_TERM",
            start_dt=start,
            end_dt=end,
            signed_dt=signed,
            rent=rent,
            extra_role=extra_role,
            payments=_build_payments(start, end, now, rent, rng),
        )
        leases.append(lease)

        if indefinite or end is None or end >= now:
            break  # currently occupied
        if rng.random() < 0.65:
            cursor = end + timedelta(days=rng.randint(0, 90))  # re-let after a gap
        else:
            break  # property left vacant

    if leases:
        last = leases[-1]
        occupied = last.end_dt is None or last.end_dt > now
        vacant_status = rng.choice(["VACANT", "VACANT", "MAINTENANCE", "LISTED"])
        status = "OCCUPIED" if occupied else vacant_status
    else:
        status = rng.choice(["VACANT", "SELF_OCCUPIED"])

    return PropertyPlan(created_dt=created, status=status, leases=leases)


def build_plans(cfg: Config, now: datetime) -> list[UserPlan]:
    window_start = now - relativedelta(months=cfg.months)
    window_days = max(1, (now - window_start).days)
    cohorts = _cohort_sizes(cfg.users, cfg.months, cfg.growth)

    plans: list[UserPlan] = []
    index = 0
    for month_i, size in enumerate(cohorts):
        month_start = window_start + relativedelta(months=month_i)
        month_end = min(month_start + relativedelta(months=1), now)
        span = max(1.0, (month_end - month_start).total_seconds())
        for _ in range(size):
            rng = _rng_for(cfg.seed, index)
            signup = month_start + timedelta(seconds=rng.random() * span)
            if signup >= now:
                signup = now - timedelta(hours=1)
            code, locale, phone_cc, _w, rent_base = pick_country(rng)

            tenure_frac = (now - signup).days / window_days
            engagement = _clamp(rng.gauss(1.0, 0.45), 0.15, 2.6)
            n_props = int(round(_clamp(1 + engagement * 1.4 + tenure_frac * engagement * 2.2,
                                       1, cfg.max_properties)))

            user = UserPlan(
                index=index,
                seed=cfg.seed * 1_000_003 + index,
                signup_dt=signup,
                country=code,
                locale=locale,
                phone_cc=phone_cc,
                currency="EUR" if code != "GB" else "GBP",
                org_contacts=rng.randint(1, 4),
            )
            for _p in range(n_props):
                user.properties.append(_build_property(signup, now, rent_base, cfg, rng))
            plans.append(user)
            index += 1
    return plans


def summarize(plans: list[UserPlan], cfg: Config) -> dict:
    props = contacts = contracts = payments = 0
    documents = photos = 0
    for u in plans:
        props += len(u.properties)
        contacts += u.org_contacts
        for p in u.properties:
            if cfg.photos:
                photos += 1
            if cfg.documents:
                documents += 1  # property doc
            for lease in p.leases:
                contracts += 1
                contacts += 1 + (1 if lease.extra_role else 0)
                payments += len(lease.payments)
                if cfg.documents:
                    documents += 1  # contract doc
                    documents += sum(1 for pay in lease.payments if pay.paid)  # receipts
    if cfg.documents:
        documents += contacts  # one doc per contact
    api_calls = (
        len(plans)  # registrations (+ token/verify not counted)
        + props + contacts + contracts + payments + documents + photos
    )
    return {
        "users": len(plans),
        "properties": props,
        "contacts": contacts,
        "contracts": contracts,
        "payments": payments,
        "documents": documents,
        "photos": photos,
        "approx_api_calls": api_calls,
    }
