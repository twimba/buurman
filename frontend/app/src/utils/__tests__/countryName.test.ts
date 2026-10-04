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

  it('returns an empty string for malformed codes', () => {
    expect(formatCountryName('not-a-code', 'en')).toBe('');
  });

  it('returns an empty string for unassigned regions', () => {
    expect(formatCountryName('ZZ', 'en')).toBe('');
    expect(formatCountryName('XY', 'en')).toBe('');
  });

  it.each(['XA', 'XB', 'EU', 'UN', 'QO', 'ZZ', 'XX'])(
    'returns an empty string for the pseudo or unlisted region %s',
    (code) => {
      expect(formatCountryName(code, 'en')).toBe('');
      expect(formatCountryName(code.toLowerCase(), 'en')).toBe('');
    }
  );

  it.each([
    ['BE', 'Belgium'],
    ['JP', 'Japan'],
    ['IT', 'Italy'],
  ])('names the listed country %s', (code, name) => {
    expect(formatCountryName(code, 'en')).toBe(name);
  });

  it('returns an empty string for an invalid language tag', () => {
    expect(formatCountryName('IT', 'xx_invalid_')).toBe('');
  });

  it('returns an empty string when there is no code', () => {
    expect(formatCountryName(undefined, 'en')).toBe('');
    expect(formatCountryName('', 'en')).toBe('');
  });
});
