import { getCountryByCode } from '@/utils/countries';

/**
 * Localized nominative country name for an ISO 3166-1 alpha-2 code, or an empty string when the
 * code is not one of the app's own countries (the property picker's list) or the runtime cannot
 * name it. Intl also names pseudo regions (EU, UN, XA, XB, QO, ...), so the app list is the gate
 * that lets callers hide the row instead of printing something that is not a country.
 */
export const formatCountryName = (
  code: string | undefined,
  language: string
): string => {
  if (!code) {
    return '';
  }
  const upper = code.toUpperCase();
  if (!getCountryByCode(upper)) {
    return '';
  }
  try {
    return (
      new Intl.DisplayNames([language], {
        type: 'region',
        fallback: 'none',
      }).of(upper) ?? ''
    );
  } catch {
    return '';
  }
};
