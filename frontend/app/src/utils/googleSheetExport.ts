/**
 * Per-call timeout overrides for Google Sheets exports. The generated booklet/report endpoints hit
 * Google Drive (folder lookup + title check + move) plus Sheets (create + values batchUpdate +
 * formatting batchUpdate) — 5-7 sequential Google API calls. Each can take 0.5-2s, so we override
 * the default 15s axios timeout to a generous 60s.
 */
export const GOOGLE_SHEET_EXPORT_TIMEOUT_MS = 60_000;

/**
 * Takeout export: same flow but with 22 tabs (one per entity) plus a row dump per table —
 * comfortably the heaviest export we offer. 3 min upper bound.
 */
export const GOOGLE_SHEET_TAKEOUT_TIMEOUT_MS = 180_000;
