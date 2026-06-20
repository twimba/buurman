# Backoffice Cost Tracking — design + plan

Adds infra/comms **cost visibility + insights** to the backoffice: a mission-control
**Cost watch** panel (summary) and a dedicated **Costs** page (breakdown, trend, insights).

## Decisions (confirmed)
1. Panel **and** a dedicated Costs page.
2. Base currency **EUR** (everything normalized).
3. Mix **Actual / Estimated / Subscription** costs, each badged by confidence.
4. Persist **daily snapshots** (history table + Quartz job) — required for trend/MoM/insights.
5. Phase 1 providers: **Twilio (actual)** + **Hetzner (run-rate estimate)** live; **Cloudflare / Better Stack / Mailgun** as configured monthly EUR amounts. Phase 2 upgrades each to its API.
6. Tokens added as env where a real API exists.

## Provider data reality
| Provider | Phase-1 source | Type |
|---|---|---|
| Twilio | REST `/Usage/Records/ThisMonth` (basic auth, existing creds) | ACTUAL |
| Hetzner | `/v1/servers`+`/v1/volumes`+`/v1/pricing` → monthly run-rate (token `HETZNER_BEARER_TOKEN`) | ESTIMATED |
| Cloudflare | configured monthly EUR (`backoffice.cost.manual-eur.cloudflare`) | SUBSCRIPTION |
| Better Stack | configured monthly EUR | SUBSCRIPTION |
| Mailgun | configured monthly EUR (volume API in Phase 2) | SUBSCRIPTION |

## Architecture
- `CostSource` SPI per provider → `ProviderReading{provider, type, currency, amountMinor, breakdown[], available, note}`. Any failure / missing token → `available=false` (panel shows a "connect" CTA, never fake numbers).
- `FxConverter` normalizes original currency → EUR via configurable static rates (`backoffice.cost.fx`).
- `CostSnapshotJob` (Quartz, daily) reads every source, normalizes, writes one `cost_snapshot` row per provider (original + EUR + fx rate + breakdown JSONB + `period_month`).
- `CostService`:
  - `costWatch()` → dashboard panel: total monthly EUR, MoM %, top providers, one headline insight.
  - `overview()` → Costs page: per-provider breakdown (badged), 6-month trend, insights, freshness.
- Endpoints: `GET /backoffice/dashboard/cost-watch` (replaces PREVIEW), `GET /backoffice/cost/overview`, `POST /backoffice/cost/refresh` (admin, snapshot now).

## Insights (the point)
- Total monthly run-rate (EUR) + MoM change.
- **Cost-to-serve per active team** = total ÷ active teams.
- **Cost per notification** = (Twilio + Mailgun) ÷ notifications sent this month.
- Biggest mover MoM.
(MoM/trend insights populate once ≥1 prior month of snapshots exists.)

## Data model — `V058__cost_tracking.sql`
`cost_snapshot(id, provider, source_type, captured_at, period_month, currency, amount_minor, amount_eur_minor, fx_rate, breakdown JSONB, created_at)` + index on `(provider, captured_at DESC)` and `(period_month)`. Daily rows; queries take the latest per provider (current) and latest per (provider, month) (trend). Additive, rollback-safe.

## Out of scope (Phase 2)
Editable manual amounts via UI; Mailgun/Cloudflare/Better Stack live APIs; FX auto-refresh; cost alerts/budgets; cost vs MRR (needs billing).
