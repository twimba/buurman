/**
 * Localized nominative country name for an ISO 3166-1 alpha-2 code, or an empty string when there
 * is no code or the runtime cannot name the region (malformed or unassigned code, invalid
 * language), so callers can hide the row instead of printing a bare code.
 */
export const formatCountryName = (
  code: string | undefined,
  language: string
): string => {
  if (!code) {
    return '';
  }
  // ZZ is CLDR's "unknown region" placeholder and resolves to a generic label, not a country.
  if (code.toUpperCase() === 'ZZ') {
    return '';
  }
  try {
    return (
      new Intl.DisplayNames([language], {
        type: 'region',
        fallback: 'none',
      }).of(code.toUpperCase()) ?? ''
    );
  } catch {
    return '';
  }
};
