"""CLI entrypoint: python -m seed_data [options]."""

from __future__ import annotations

import argparse
import sys
import time
from contextlib import nullcontext
from datetime import datetime

from . import backdate, flags
from .config import BACKOFFICE_CLIENT_ID, build_config
from .runner import Runner
from .simulation import build_plans, summarize


def _parse_args(argv: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(
        prog="seed-data",
        description="Seed a local Buurman workspace with production-like data via the API.",
    )
    p.add_argument("--users", type=int, default=1000, help="number of users/teams (default 1000)")
    p.add_argument("--months", type=int, default=24, help="usage window in months (default 24)")
    p.add_argument(
        "--growth",
        choices=["exponential", "logistic", "linear"],
        default="exponential",
        help="signup growth curve (default exponential)",
    )
    p.add_argument("--seed", type=int, default=42, help="RNG seed for reproducibility")
    p.add_argument("--concurrency", type=int, default=6, help="parallel users (default 6)")

    p.add_argument("--api-url", default="", help="overrides $SEED_API_URL")
    p.add_argument("--keycloak-url", default="", help="overrides $SEED_KEYCLOAK_URL")
    p.add_argument("--mailpit-url", default="", help="overrides $SEED_MAILPIT_URL")
    p.add_argument("--db-url", default="", help="overrides $SEED_DB_URL (for backdating)")

    docs = p.add_mutually_exclusive_group()
    docs.add_argument("--documents", dest="documents", action="store_true", default=True)
    docs.add_argument("--no-documents", dest="documents", action="store_false")
    photos = p.add_mutually_exclusive_group()
    photos.add_argument("--photos", dest="photos", action="store_true", default=True)
    photos.add_argument("--no-photos", dest="photos", action="store_false")
    back = p.add_mutually_exclusive_group()
    back.add_argument("--backdate", dest="backdate", action="store_true", default=True)
    back.add_argument("--no-backdate", dest="backdate", action="store_false")

    flag = p.add_mutually_exclusive_group()
    flag.add_argument(
        "--manage-invitation-flag", dest="manage_invitation_flag",
        action="store_true", default=True,
        help="auto-disable the global invitation_required flag for the run, then restore it",
    )
    flag.add_argument(
        "--no-manage-invitation-flag", dest="manage_invitation_flag", action="store_false",
        help="don't touch the invitation_required flag (you've disabled it yourself)",
    )
    p.add_argument("--backoffice-user", default="", help="overrides $SEED_BACKOFFICE_USER")
    p.add_argument("--backoffice-password", default="", help="overrides $SEED_BACKOFFICE_PASSWORD")

    p.add_argument("--skip-email-verify", action="store_true", help="skip Mailpit verification")
    p.add_argument("--dry-run", action="store_true", help="print planned volume; no API/DB calls")
    p.add_argument("--out-dir", default=".out", help="where to write users.csv (default .out)")
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    cfg = build_config(_parse_args(sys.argv[1:] if argv is None else argv))
    now = datetime.now()

    print(
        f"Buurman seeder · {cfg.users} users over {cfg.months}mo · growth={cfg.growth} "
        f"· seed={cfg.seed} · concurrency={cfg.concurrency}"
    )
    plans = build_plans(cfg, now)
    summary = summarize(plans, cfg)
    print("Planned volume:")
    for key, value in summary.items():
        print(f"  {key:>18}: {value:,}")

    if cfg.dry_run:
        print("\n(dry run — nothing was created)")
        return 0

    print(f"\nTarget API: {cfg.api_url}")
    print(f"Creating data (verify_email={cfg.verify_email}, documents={cfg.documents}, "
          f"photos={cfg.photos}) ...")

    if cfg.manage_invitation_flag:
        flag_ctx = flags.invitation_required_disabled(
            api_url=cfg.api_url,
            keycloak_url=cfg.keycloak_url,
            username=cfg.backoffice_user,
            password=cfg.backoffice_password,
            client_id=BACKOFFICE_CLIENT_ID,
        )
    else:
        flag_ctx = nullcontext()

    collector = backdate.Collector()
    runner = Runner(cfg, now, collector)
    started = time.time()
    try:
        with flag_ctx:
            try:
                stats = runner.run(plans)
            finally:
                runner.close()
    except flags.FlagAdminError as exc:
        runner.close()
        print(f"\nCould not manage the invitation_required flag: {exc}")
        print("Disable it yourself (Backoffice -> Feature Flags) and re-run with "
              "--no-manage-invitation-flag, or pass --backoffice-user/--backoffice-password.")
        return 1

    elapsed = time.time() - started
    print(
        f"\nCreated: {stats.users_ok} users ({stats.users_failed} failed) · "
        f"{stats.properties} properties · {stats.contacts} contacts · "
        f"{stats.contracts} contracts · {stats.payments} payments · "
        f"{stats.documents} documents · {stats.photos} photos · {elapsed/60:.1f}m"
    )
    print(f"Credentials written to {runner._creds_path}")

    if cfg.backdate:
        print(f"Backdating {collector.total():,} timestamps in the database ...")
        try:
            updated = backdate.apply(cfg.db_url, collector)
            print(f"Backdated {updated:,} rows.")
        except Exception as exc:  # noqa: BLE001
            print(f"  ! backdating failed: {str(exc)[:200]}")
            print("    Data was created via the API but timestamps remain 'now'.")
            return 1
    else:
        print("Backdating skipped (--no-backdate); created_at timestamps are 'now'.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
