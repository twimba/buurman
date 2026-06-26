"""Faker-backed realistic field + request-body generators.

All functions are deterministic given the (faker, rng) passed in, so a fixed
--seed reproduces the whole dataset.
"""

from __future__ import annotations

import random
from functools import lru_cache

from faker import Faker

# (countryCode, faker_locale, phone_cc, signup_weight, rent_baseline_eur)
COUNTRIES = [
    ("NL", "nl_NL", "31", 40, 1400),
    ("DE", "de_DE", "49", 14, 1100),
    ("BE", "nl_BE", "32", 9, 1050),
    ("FR", "fr_FR", "33", 8, 1200),
    ("GB", "en_GB", "44", 7, 1600),
    ("ES", "es_ES", "34", 6, 900),
    ("IT", "it_IT", "39", 5, 950),
    ("PT", "pt_PT", "351", 3, 800),
    ("SE", "sv_SE", "46", 3, 1150),
    ("PL", "pl_PL", "48", 3, 700),
    ("IE", "en_IE", "353", 2, 1700),
    ("AT", "de_AT", "43", 2, 1050),
]

RESIDENTIAL_TYPES = ["APARTMENT", "HOUSE", "STUDIO", "ROOM", "VILLA", "TOWNHOUSE"]
COMMERCIAL_TYPES = ["OFFICE", "RETAIL", "RESTAURANT", "CAFE", "SHOWROOM"]

CONTACT_TAGS = ["VIP", "LONG_TERM", "PROSPECT", "REFERRED", "KEY_HOLDER"]


@lru_cache(maxsize=64)
def _faker_for(locale: str, seed: int) -> Faker:
    fk = Faker(locale)
    fk.seed_instance(seed)
    return fk


def pick_country(rng: random.Random) -> tuple[str, str, str, int, int]:
    weights = [c[3] for c in COUNTRIES]
    return rng.choices(COUNTRIES, weights=weights, k=1)[0]


def faker_for_country(locale: str, seed: int) -> Faker:
    return _faker_for(locale, seed)


def _email(first: str, last: str, rng: random.Random, domain: str = "seed.buurman.io") -> str:
    slug = f"{first}.{last}".lower()
    slug = "".join(ch for ch in slug if ch.isalnum() or ch == ".")
    return f"{slug}.{rng.randrange(100000, 999999)}@{domain}"


def _phone(phone_cc: str, rng: random.Random) -> str:
    digits = "".join(str(rng.randint(0, 9)) for _ in range(9))
    return f"+{phone_cc}6{digits[:8]}"


# --- request bodies ------------------------------------------------------


def property_body(fk: Faker, rng: random.Random, country: str, status: str) -> dict:
    commercial = rng.random() < 0.15
    category = "COMMERCIAL" if commercial else "RESIDENTIAL"
    ptype = rng.choice(COMMERCIAL_TYPES if commercial else RESIDENTIAL_TYPES)
    return {
        "propertyCategory": category,
        "propertyType": ptype,
        "status": status,
        "street": fk.street_address(),
        "city": fk.city(),
        "postalCode": fk.postcode(),
        "countryCode": country,
        "areaValue": rng.randint(35, 220),
        "areaUnit": "SQM",
        "yearBuilt": rng.randint(1900, 2023),
        "numberOfFloors": rng.randint(1, 6),
        "parkingSpaces": rng.choice([0, 0, 1, 2]),
        "hasSmokeDetectors": True,
        "energyEfficiencyRating": rng.choice(["A", "B", "C", "D", "E"]),
    }


def person_contact_body(fk: Faker, rng: random.Random, phone_cc: str) -> tuple[dict, str]:
    first = fk.first_name()
    last = fk.last_name()
    email = _email(first, last, rng)
    body = {
        "contactType": "INDIVIDUAL",
        "firstName": first,
        "lastName": last,
        "email": email,
        "phone": _phone(phone_cc, rng),
    }
    if rng.random() < 0.3:
        body["tags"] = [rng.choice(CONTACT_TAGS)]
    return body, f"{first} {last}"


def org_contact_body(fk: Faker, rng: random.Random, phone_cc: str) -> dict:
    is_company = rng.random() < 0.6
    name = fk.company()
    return {
        "contactType": "COMPANY" if is_company else "SERVICE_PROVIDER",
        "companyName": name,
        "email": _email(name.split()[0], "office", rng),
        "phone": _phone(phone_cc, rng),
    }


def contract_body(
    *,
    property_id: str,
    primary_contact_id: str,
    extra_party: tuple[str, str] | None,
    contract_type: str,
    start_date: str,
    end_date: str | None,
    signed_date: str | None,
    rent: int,
    currency: str,
    rng: random.Random,
) -> dict:
    parties = [{"contactIdentifier": primary_contact_id, "role": "PRIMARY_TENANT"}]
    if extra_party is not None:
        contact_id, role = extra_party
        parties.append({"contactIdentifier": contact_id, "role": role})

    body = {
        "propertyIdentifier": property_id,
        "parties": parties,
        "contractType": contract_type,
        "startDate": start_date,
        "rentAmount": rent,
        "rentAmountCurrency": currency,
        "paymentFrequency": "MONTHLY",
        "paymentDueDay": 1,
        "depositAmount": rent * rng.choice([1, 2]),
        "securityDeposit": 0,
    }
    if end_date is not None:
        body["endDate"] = end_date
    if signed_date is not None:
        body["signedDate"] = signed_date
    return body


def payment_body(
    *,
    contract_id: str,
    contact_id: str,
    amount: int,
    currency: str,
    due_date: str,
    paid: bool,
    payment_date: str | None,
) -> dict:
    body = {
        "contractIdentifier": contract_id,
        "contactIdentifier": contact_id,
        "amount": amount,
        "currency": currency,
        "dueDate": due_date,
    }
    if paid and payment_date is not None:
        body["markAsPaid"] = True
        body["paymentDate"] = payment_date
    return body
