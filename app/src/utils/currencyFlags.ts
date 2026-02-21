/**
 * Derives a flag emoji from an ISO 4217 currency code.
 *
 * Strategy: take the first two characters of the currency code (which almost
 * always match the ISO 3166-1 alpha-2 country code) and convert them to
 * Unicode regional indicator symbols.
 *
 * Special cases are handled for currencies where the first two characters
 * don't map to the issuing country (e.g. XAF, XOF, XCD).
 */

const COUNTRY_OVERRIDES: Record<string, string> = {
  // Supranational / special codes → representative flag
  EUR: 'EU',
  XAF: 'CM', // Central African CFA → Cameroon
  XOF: 'SN', // West African CFA → Senegal
  XCD: 'AG', // East Caribbean Dollar → Antigua
  XPF: 'PF', // CFP Franc → French Polynesia
  ANG: 'CW', // Netherlands Antillean Guilder → Curaçao
};

/**
 * Convert a two-letter country code to its flag emoji.
 * Each ASCII letter A-Z maps to a regional indicator symbol U+1F1E6..U+1F1FF.
 */
const countryToFlag = (countryCode: string): string => {
  const upper = countryCode.toUpperCase();
  const first = 0x1f1e6 + (upper.charCodeAt(0) - 65);
  const second = 0x1f1e6 + (upper.charCodeAt(1) - 65);
  return String.fromCodePoint(first, second);
};

/**
 * Returns a flag emoji for the given ISO 4217 currency code.
 * Falls back to a generic banknote emoji for unknown/unmappable codes.
 */
export const getCurrencyFlag = (code: string): string => {
  if (!code || code.length < 2) return '\uD83D\uDCB5'; // 💵
  const country = COUNTRY_OVERRIDES[code.toUpperCase()] ?? code.slice(0, 2);
  return countryToFlag(country);
};
