import { describe, expect, it } from 'vitest';
import { formatCountryName } from '../countryName';

describe('formatCountryName', () => {
  it('returns the localized nominative name', () => {
    expect(formatCountryName('IT', 'en')).toBe('Italy');
    expect(formatCountryName('IT', 'nl')).toBe('Italië');
    expect(formatCountryName('NL', 'fi')).toBe('Alankomaat');
  });

  it('accepts lowercase codes', () => {
    expect(formatCountryName('it', 'en')).toBe('Italy');
  });

  it('falls back to the code for malformed codes', () => {
    expect(formatCountryName('not-a-code', 'en')).toBe('not-a-code');
  });

  it('falls back to the code for an invalid language tag', () => {
    expect(formatCountryName('IT', 'xx_invalid_')).toBe('IT');
  });

  it('returns an empty string when there is no code', () => {
    expect(formatCountryName(undefined, 'en')).toBe('');
    expect(formatCountryName('', 'en')).toBe('');
  });
});
