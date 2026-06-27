const EUR = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 2,
});

/** Formats EUR minor units (cents) as a currency string, e.g. 123456 -> "€1,234.56". */
export const formatEurMinor = (minor: number): string =>
  EUR.format(minor / 100);

/** Compact EUR for tight spaces, e.g. 123456 -> "€1.2K". */
export const formatEurMinorCompact = (minor: number): string => {
  const eur = minor / 100;
  if (Math.abs(eur) >= 1000) {
    return `€${(eur / 1000).toFixed(1)}K`;
  }
  return EUR.format(eur);
};

/**
 * Parses a user-typed non-negative decimal, tolerating the European comma (e.g. "1.234,56" or
 * "1234.56"). Returns null for blank/invalid/negative input so callers can keep an editor open and
 * surface a message rather than persisting NaN. Full precision is preserved (no cent rounding) — use
 * this for sub-cent rates like a per-email fee.
 */
export const parseDecimal = (raw: string): number | null => {
  const cleaned = raw.includes(',')
    ? raw.trim().replace(/\s/g, '').replace(/\./g, '').replace(',', '.')
    : raw.trim();
  const value = parseFloat(cleaned);
  if (Number.isNaN(value) || value < 0) {
    return null;
  }
  return value;
};

/** Parses a user-typed EUR amount into minor units (cents). Null on invalid/negative input. */
export const parseEurToMinor = (raw: string): number | null => {
  const value = parseDecimal(raw);
  return value === null ? null : Math.round(value * 100);
};

/** Seeds a money editor from minor units, e.g. 123450 -> "1234.50" (keeps both cents digits). */
export const eurMinorToInput = (minor: number): string =>
  (minor / 100).toFixed(2);
