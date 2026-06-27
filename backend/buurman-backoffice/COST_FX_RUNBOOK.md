# Cost & FX Operations Runbook

Operational notes for the backoffice Cost panel, the Costs page, and FX normalization.

## 1. Cost provider shows UNAVAILABLE / "Awaiting first snapshot"

A provider is UNAVAILABLE when its API source returns no reading (missing/invalid
credentials, upstream error) and no manual amount is configured. "Awaiting first
snapshot" means no snapshot row exists yet for the current month.

Checklist:

1. Confirm the daily snapshot job ran (or trigger a manual refresh from the Cost
   panel — `BACKOFFICE_ADMIN` only). The refresh fans out to every provider over
   HTTP, then persists.
2. Verify credentials/env per provider:
   - **Hetzner** — `HETZNER_BEARER_TOKEN`
   - **Cloudflare** — API token **and** account id
   - **Twilio** — account SID + auth token
   - **Mailgun** — API key (and region/base URL if non-default)
3. Check backend logs for the provider name around snapshot time (read failures and
   `No FX rate for <provider>` skips are logged at WARN).
4. Fallback: set a **manual monthly EUR amount** on the Costs page. A manual amount
   is used whenever the live API reading is unavailable, so the provider still
   contributes to the run-rate total.

## 2. FX normalization

- All provider costs are normalized to EUR. The live source is **frankfurter.app**
  (ECB data, no API key, HTTPS enforced).
- **Daily refresh** fetches today's EUR rate for every tracked pair plus any
  config-only currency; a per-currency failure logs and keeps the last stored rate.
- **Backfill** pulls daily history per currency. The range is **clamped to the last
  2 years** (`MAX_BACKFILL_YEARS`) so a far-past `since` can't request an unbounded
  span / row count.
- **Snapshots freeze EUR at capture-time FX.** `amount_eur_minor` records the rate
  effective on the capture day and is the historical record. Backfilling or
  correcting a past rate does **NOT** re-normalize existing snapshots — it only
  affects future captures. To restate history you must re-snapshot.
- Manual rate override and per-day rate delete are available on the FX view
  (`BACKOFFICE_ADMIN`).

## 3. Prometheus / metrics panels

- Metrics-backed panels query Prometheus through a **circuit breaker with a 30s
  cooldown** after a failure: once a call fails, further calls are short-circuited
  for 30s instead of repeatedly hammering a down Prometheus.
- While Prometheus is unreachable (or the breaker is open), affected panels
  **degrade to PREVIEW** rather than erroring. Recovery is automatic once Prometheus
  responds again after the cooldown.

## 4. Live-tail caveat (clustered deploy)

The dashboard "Live tail" reads an **in-memory, per-JVM-instance** ring buffer. In a
multi-replica deployment it only shows logs from the instance that served the
request, not the whole fleet. For fleet-wide tailing use a centralized log sink
(Loki/ELK), not this panel.

## 5. Suggested staleness alerts (to add)

- **Cost snapshot age** — alert if the newest cost snapshot (`captured_at`) is older
  than ~36h, indicating the daily job stopped or every provider is failing.
- **FX rate age** — alert if the newest stored FX rate is older than ~3 days
  (accounting for ECB weekend/holiday gaps), indicating FX refresh is broken and
  conversions are silently using stale rates.
