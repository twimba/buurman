import { getCurrencySymbol } from './currencies';

/**
 * Format a monetary amount.
 *
 * Uses `Intl.NumberFormat` with `currencyDisplay: 'narrowSymbol'` so EUR renders as `€`,
 * USD as `$`, etc., never the ISO code prefix (`EUR1,234.00`) — which was the bug visible
 * in the mobile review screenshots.
 *
 * Browsers without `narrowSymbol` support (older Safari) fall back to our local symbol map.
 *
 * @param amount    Numeric value
 * @param currency  ISO 4217 code (`EUR`, `USD`, …)
 * @param options   Locale + fraction-digit overrides
 */
export function formatMoney(
  amount: number,
  currency: string,
  options: {
    locale?: string;
    minimumFractionDigits?: number;
    maximumFractionDigits?: number;
  } = {}
): string {
  const {
    locale = typeof navigator !== 'undefined' ? navigator.language : 'en-US',
    minimumFractionDigits = 2,
    maximumFractionDigits = 2,
  } = options;

  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      currencyDisplay: 'narrowSymbol',
      minimumFractionDigits,
      maximumFractionDigits,
    }).format(amount);
  } catch {
    // narrowSymbol unsupported → manual symbol prefix
    const symbol = getCurrencySymbol(currency);
    return `${symbol}${amount.toLocaleString(locale, {
      minimumFractionDigits,
      maximumFractionDigits,
    })}`;
  }
}

/**
 * Compact representation for tight UI (mobile KPI tiles, chart labels).
 *
 * | Range            | Format          | Example     |
 * |------------------|-----------------|-------------|
 * | `≥ 10_000_000`   | `€X.XM`         | `€9.7M`     |
 * | `1M – 9.99M`     | `€X.XXM`        | `€1.66M`    |
 * | `100K – 999K`    | `€XXXK`         | `€124K`     |
 * | `< 100K`         | full, no decimals| `€33,779`  |
 */
export function formatMoneyCompact(
  amount: number,
  currency: string,
  locale: string = typeof navigator !== 'undefined'
    ? navigator.language
    : 'en-US'
): string {
  const abs = Math.abs(amount);
  const sign = amount < 0 ? '-' : '';
  const symbol = getCurrencySymbol(currency);

  if (abs >= 10_000_000) {
    return `${sign}${symbol}${(abs / 1_000_000).toFixed(1)}M`;
  }
  if (abs >= 1_000_000) {
    return `${sign}${symbol}${(abs / 1_000_000).toFixed(2)}M`;
  }
  if (abs >= 100_000) {
    return `${sign}${symbol}${Math.round(abs / 1_000)}K`;
  }
  return formatMoney(amount, currency, {
    locale,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
}
