/**
 * Localized nominative country name for an ISO 3166-1 alpha-2 code. Falls back to the code itself
 * when the runtime cannot resolve it (malformed code or language), and to an empty string when
 * there is no code at all.
 */
export const formatCountryName = (
  code: string | undefined,
  language: string
): string => {
  if (!code) {
    return '';
  }
  try {
    return (
      new Intl.DisplayNames([language], { type: 'region' }).of(
        code.toUpperCase()
      ) ?? code
    );
  } catch {
    return code;
  }
};
