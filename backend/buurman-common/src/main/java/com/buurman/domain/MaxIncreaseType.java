package com.buurman.domain;

/**
 * Type of rent-increase regulation seeded by V053__rent_regulation_overhaul.sql.
 *
 * <p>The migration encodes per-country rent regulation rules; this enum is the canonical set of
 * regulation regimes. New values added here MUST also be added to {@code openapi/src/app.yaml}'s
 * {@code MaxIncreaseType} schema (and re-bundled with {@code make bundle-openapi}) so the
 * frontend's generated TS stays in sync.
 *
 * <p>Coverage by country (see V053 for full notes): - FIXED_PERCENTAGE: fixed % rate, e.g. NL Wet
 * betaalbare huur - CPI_LINKED: indexed to consumer price index - INDEX_LINKED: indexed to another
 * official series (cost, rent, etc.) - MARKET_RENT: comparable-rent regulation (e.g. DE
 * Mietspiegel) - MARKET: free-market / unregulated (e.g. DK post-1991 fri leje) - FREE_MARKET:
 * explicit free-market designation in regulated regimes - NEGOTIATED: bilateral negotiation between
 * landlord/tenant - FROZEN: rent freeze (statutory or emergency) - STATUTORY_CAP: hard cap defined
 * by law (e.g. DE Kappungsgrenze §558 BGB) - STATUTORY_CEILING: ceiling rent regime - CEILING_RENT: loyer de référence
 * majoré ceiling (e.g. FR encadrement des loyers) - COMPARATIVE:
 * comparable-rent test (similar to MARKET_RENT but stricter) - COST_BASED: cost-recovery regulation
 * - CAPITAL_BASED: capital-value-based formula - FORMULA_BASED: bespoke formula (e.g. CH IRPL) -
 * COST_PASS_THROUGH: modernisation cost pass-through (e.g. DE §559 BGB) - CAP_OVER_INDEX: cap
 * applied on top of an index - CANTONAL_RESTRICTION: cantonal/regional restriction (e.g. CH) -
 * THRESHOLD_PERCENTAGE: threshold-triggered % increase - OTHER: fallback for regimes that don't fit
 * any of the above
 */
public enum MaxIncreaseType {
  FIXED_PERCENTAGE,
  CPI_LINKED,
  INDEX_LINKED,
  MARKET_RENT,
  MARKET,
  FREE_MARKET,
  NEGOTIATED,
  FROZEN,
  STATUTORY_CAP,
  STATUTORY_CEILING,
  CEILING_RENT,
  COMPARATIVE,
  COST_BASED,
  CAPITAL_BASED,
  FORMULA_BASED,
  COST_PASS_THROUGH,
  CAP_OVER_INDEX,
  CANTONAL_RESTRICTION,
  THRESHOLD_PERCENTAGE,
  OTHER
}
