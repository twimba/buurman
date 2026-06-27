"""Faker-backed realistic field + request-body generators.

All functions are deterministic given the (faker, rng) passed in, so a fixed
--seed reproduces the whole dataset.
"""

from __future__ import annotations

import random
from functools import lru_cache

from faker import Faker

# (countryCode, faker_locale, phone_cc, signup_weight, rent_baseline_eur, doc_language)
# Heavily weighted toward countries whose primary language Buurman supports
# (da de el en es fi fr it nb nl pl pt sv), with a long tail of others worldwide
# for global spread. Amounts stay EUR (single team currency is enforced backend-side);
# only geography/locale/document-language vary.
COUNTRIES = [
    # --- Dutch ---
    ("NL", "nl_NL", "31", 34, 1400, "nl"),
    ("BE", "nl_BE", "32", 9, 1100, "nl"),
    # --- German ---
    ("DE", "de_DE", "49", 16, 1150, "de"),
    ("AT", "de_AT", "43", 4, 1100, "de"),
    ("CH", "de_CH", "41", 3, 1900, "de"),
    # --- English ---
    ("GB", "en_GB", "44", 10, 1600, "en"),
    ("IE", "en_IE", "353", 3, 1700, "en"),
    ("US", "en_US", "1", 12, 1500, "en"),
    ("CA", "en_CA", "1", 5, 1400, "en"),
    ("AU", "en_AU", "61", 4, 1700, "en"),
    ("NZ", "en_NZ", "64", 2, 1500, "en"),
    # --- Spanish ---
    ("ES", "es_ES", "34", 7, 950, "es"),
    ("MX", "es_MX", "52", 4, 700, "es"),
    ("AR", "es_AR", "54", 2, 500, "es"),
    ("CO", "es_CO", "57", 2, 550, "es"),
    ("CL", "es_CL", "56", 2, 650, "es"),
    # --- French ---
    ("FR", "fr_FR", "33", 9, 1200, "fr"),
    ("LU", "fr_FR", "352", 1, 1800, "fr"),
    # --- Italian ---
    ("IT", "it_IT", "39", 6, 950, "it"),
    # --- Portuguese ---
    ("PT", "pt_PT", "351", 3, 800, "pt"),
    ("BR", "pt_BR", "55", 5, 600, "pt"),
    # --- Nordic (Swedish / Norwegian / Danish / Finnish) ---
    ("SE", "sv_SE", "46", 4, 1150, "sv"),
    ("NO", "no_NO", "47", 3, 1500, "nb"),
    ("DK", "da_DK", "45", 3, 1400, "da"),
    ("FI", "fi_FI", "358", 2, 1200, "fi"),
    # --- Polish / Greek ---
    ("PL", "pl_PL", "48", 4, 750, "pl"),
    ("GR", "el_GR", "30", 2, 700, "el"),
    # --- Long tail worldwide (document language falls back to English) ---
    ("JP", "ja_JP", "81", 2, 1300, "en"),
    ("IN", "en_IN", "91", 3, 400, "en"),
    ("ZA", "en_ZA", "27", 1, 600, "en"),
    ("AE", "ar_AA", "971", 1, 1800, "en"),
    ("RU", "ru_RU", "7", 1, 700, "en"),
    ("TR", "tr_TR", "90", 1, 600, "en"),
    ("ID", "id_ID", "62", 1, 350, "en"),
    ("KR", "ko_KR", "82", 1, 1200, "en"),
    ("SG", "en_US", "65", 1, 2000, "en"),
]

RESIDENTIAL_TYPES = ["APARTMENT", "HOUSE", "STUDIO", "ROOM", "VILLA", "TOWNHOUSE"]
COMMERCIAL_TYPES = ["OFFICE", "RETAIL", "RESTAURANT", "CAFE", "SHOWROOM"]

CONTACT_TAGS = ["VIP", "LONG_TERM", "PROSPECT", "REFERRED", "KEY_HOLDER"]


@lru_cache(maxsize=128)
def _faker_for(locale: str, seed: int) -> Faker:
    try:
        fk = Faker(locale)
    except (AttributeError, ValueError, ModuleNotFoundError):
        fk = Faker("en_US")  # fall back if this Faker build lacks the locale
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
    document_language: str,
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
        "documentLanguages": [document_language],
        "depositAmount": rent * rng.choice([1, 2]),
        "securityDeposit": 0,
        # Optional in the DTO but dereferenced unconditionally by ContractRepository.save,
        # so they must be present to avoid a backend NPE.
        "renewalMode": "NONE",
        "rentAdjustmentType": "NONE",
        # NOT NULL in the DB; the repo inserts the raw value, so omitting these
        # writes null and violates the constraint (their DB defaults don't apply).
        "terminationNoticeDays": rng.choice([30, 60, 90]),
        "landlordNoticeDays": rng.choice([30, 60, 90]),
        "tenantNoticeDays": rng.choice([30, 30, 60]),
        "requiresTenantConfirmation": False,
    }
    # Some contracts carry a percentage rent adjustment; the DB CHECK requires a paired
    # value when the type isn't NONE/MANUAL (chk_contracts_rent_adj_value_required).
    if rng.random() < 0.25:
        body["rentAdjustmentType"] = "FIXED_PERCENTAGE"
        body["rentAdjustmentValue"] = round(rng.uniform(1.5, 5.0), 1)
    if contract_type == "INDEFINITE" and rng.random() < 0.5:
        body["renewalMode"] = "AUTOMATIC"
        body["renewalTermMonths"] = 12
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
