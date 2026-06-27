"""Orchestrates per-user creation over a thread pool, with progress + credentials."""

from __future__ import annotations

import csv
import os
import random
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timedelta

from . import auth, generators
from .api import BuurmanApi
from .assets import JPEG_BYTES, pdf_bytes
from .backdate import Collector
from .config import SEED_PASSWORD, Config
from .simulation import UserPlan


@dataclass
class Stats:
    users_ok: int = 0
    users_failed: int = 0
    properties: int = 0
    contacts: int = 0
    contracts: int = 0
    payments: int = 0
    documents: int = 0
    photos: int = 0


def _iso(dt: datetime) -> str:
    return dt.date().isoformat()


class Runner:
    def __init__(self, cfg: Config, now: datetime, collector: Collector, db=None) -> None:
        self.cfg = cfg
        self.now = now
        self.collector = collector
        self.db = db  # DbClient, required when cfg.email_verify == "db"
        self.stats = Stats()
        self.run_id = uuid.uuid4().hex[:6]  # unique per run -> emails never collide on re-runs
        self._lock = threading.Lock()
        os.makedirs(cfg.out_dir, exist_ok=True)
        self._creds_path = os.path.join(cfg.out_dir, "users.csv")
        self._creds = open(self._creds_path, "w", newline="")  # noqa: SIM115
        self._writer = csv.writer(self._creds)
        self._writer.writerow(
            ["email", "password", "team_identifier", "user_identifier", "country", "signup"]
        )

    def close(self) -> None:
        self._creds.close()

    # --- per-entity helpers (record backdate as we create) ---------------

    def _doc(self, api_upload, parent_id: str, title: str, when: datetime) -> None:
        if not self.cfg.documents:
            return
        doc_id = api_upload(
            parent_id, filename=f"{title}.pdf", content=pdf_bytes(title),
            mime="application/pdf", title=title,
        )
        self.collector.add("documents", doc_id, when)
        with self._lock:
            self.stats.documents += 1

    def _photo(self, api: BuurmanApi, prop_id: str, when: datetime) -> None:
        if not self.cfg.photos:
            return
        photo_id = api.upload_property_photo(
            prop_id, filename="photo.jpg", content=JPEG_BYTES,
            mime="image/jpeg", title="Property photo",
        )
        self.collector.add("photos", photo_id, when)
        with self._lock:
            self.stats.photos += 1

    def _contact(self, api: BuurmanApi, body: dict, when: datetime, doc_title: str) -> str:
        cid = api.create_contact(body)
        self.collector.add("contacts", cid, when)
        with self._lock:
            self.stats.contacts += 1
        self._doc(api.upload_contact_document, cid, doc_title, when)
        return cid

    # --- per-user worker -------------------------------------------------

    def _run_user(self, plan: UserPlan) -> None:
        cfg = self.cfg
        rng = random.Random(plan.seed)
        fk = generators.faker_for_country(plan.locale, plan.seed)
        first = fk.first_name()
        last = fk.last_name()
        email = f"seed-{self.run_id}-{plan.index:06d}@seed.buurman.io"

        account = auth.provision_account(
            api_url=cfg.api_url,
            keycloak_url=cfg.keycloak_url,
            mailpit_url=cfg.mailpit_url,
            email=email,
            password=SEED_PASSWORD,
            first_name=first,
            last_name=last,
            mailpit_verify=(cfg.email_verify == "mailpit"),
        )
        if cfg.email_verify == "db":
            self.db.mark_email_verified(account.user_identifier, plan.signup_dt)
        self.collector.add_account(account.team_identifier, account.user_identifier, plan.signup_dt)
        with self._lock:
            self._writer.writerow(
                [email, SEED_PASSWORD, account.team_identifier, account.user_identifier,
                 plan.country, plan.signup_dt.isoformat()]
            )

        api = BuurmanApi(cfg.api_url, account.token)

        # Declare the team's country so it shows on the backoffice Geo map (grouped by team country).
        api.set_team_country(account.team_identifier, plan.country)

        # Non-tenant contacts (companies / service providers)
        span = (self.now - plan.signup_dt).total_seconds()
        for _ in range(plan.org_contacts):
            when = plan.signup_dt
            if span > 0:
                when = plan.signup_dt + timedelta(seconds=span * rng.random())
            self._contact(api, generators.org_contact_body(fk, rng, plan.phone_cc), when,
                          "Service agreement")

        for prop in plan.properties:
            body = generators.property_body(fk, rng, plan.country, prop.status)
            pid = api.create_property(body)
            self.collector.add("properties", pid, prop.created_dt)
            with self._lock:
                self.stats.properties += 1
            self._photo(api, pid, prop.created_dt)
            self._doc(api.upload_property_document, pid, "Floor plan", prop.created_dt)

            for lease in prop.leases:
                tbody, name = generators.person_contact_body(fk, rng, plan.phone_cc)
                tenant_id = self._contact(api, tbody, lease.signed_dt, "ID document")

                extra_party = None
                if lease.extra_role:
                    ebody, _ = generators.person_contact_body(fk, rng, plan.phone_cc)
                    extra_id = self._contact(api, ebody, lease.signed_dt, "ID document")
                    extra_party = (extra_id, lease.extra_role)

                contract_body = generators.contract_body(
                    property_id=pid,
                    primary_contact_id=tenant_id,
                    extra_party=extra_party,
                    contract_type=lease.contract_type,
                    start_date=_iso(lease.start_dt),
                    end_date=_iso(lease.end_dt) if lease.end_dt else None,
                    signed_date=_iso(lease.signed_dt),
                    rent=lease.rent,
                    currency=plan.currency,
                    document_language=plan.language,
                    rng=rng,
                )
                cid = api.create_contract(contract_body)
                self.collector.add("contracts", cid, lease.signed_dt)
                with self._lock:
                    self.stats.contracts += 1
                # Activate so payments can be attached (the previous lease on this
                # property is already EXPIRED/TERMINATED below, keeping one active).
                api.change_contract_status(cid, "ACTIVE")
                self._doc(api.upload_contract_document, cid, "Lease agreement", lease.signed_dt)

                for pay in lease.payments:
                    pbody = generators.payment_body(
                        contract_id=cid,
                        contact_id=tenant_id,
                        amount=pay.amount,
                        currency=plan.currency,
                        due_date=_iso(pay.due_dt),
                        paid=pay.paid,
                        payment_date=_iso(pay.payment_dt) if pay.payment_dt else None,
                    )
                    pay_id = api.create_payment(pbody)
                    self.collector.add("payments", pay_id, pay.due_dt)
                    with self._lock:
                        self.stats.payments += 1
                    if pay.paid:
                        self._doc(api.upload_payment_document, pay_id, "Receipt",
                                  pay.payment_dt or pay.due_dt)

                # A lease that already ended leaves ACTIVE for the next one on this
                # property; ongoing leases (no end / end in future) stay ACTIVE.
                if lease.end_dt is not None and lease.end_dt < self.now:
                    api.change_contract_status(
                        cid, "TERMINATED" if rng.random() < 0.15 else "EXPIRED"
                    )

    def run(self, plans: list[UserPlan]) -> Stats:
        start = time.time()
        total = len(plans)
        done = 0
        with ThreadPoolExecutor(max_workers=self.cfg.concurrency) as pool:
            futures = {pool.submit(self._run_user, p): p for p in plans}
            for fut in as_completed(futures):
                done += 1
                try:
                    fut.result()
                    with self._lock:
                        self.stats.users_ok += 1
                except Exception as exc:  # noqa: BLE001 - keep going on per-user failures
                    with self._lock:
                        self.stats.users_failed += 1
                    print(f"  ! user {futures[fut].index} failed: {str(exc)[:160]}")
                if done % 10 == 0 or done == total:
                    rate = done / max(0.001, time.time() - start)
                    eta = (total - done) / max(0.001, rate)
                    print(
                        f"  {done}/{total} users · {self.stats.properties}p "
                        f"{self.stats.contracts}c {self.stats.payments}pay "
                        f"· {rate:.1f}/s · ETA {eta/60:.1f}m",
                        flush=True,
                    )
        return self.stats
