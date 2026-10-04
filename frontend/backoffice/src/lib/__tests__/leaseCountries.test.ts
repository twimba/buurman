import { describe, expect, it } from 'vitest';
import { LEASE_COUNTRIES } from '../leaseCountries';
import { decodePreviewForm } from '../leasePreviewUrl';

describe('LEASE_COUNTRIES', () => {
  it('lists Greece so its lease clause templates can be managed', () => {
    expect(LEASE_COUNTRIES).toContainEqual({ code: 'GR', name: 'Greece' });
  });

  it('has unique ISO alpha-2 codes', () => {
    const codes = LEASE_COUNTRIES.map((c) => c.code);
    expect(new Set(codes).size).toBe(codes.length);
    codes.forEach((code) => expect(code).toMatch(/^[A-Z]{2}$/));
  });

  it('lets the preview URL select Greece', () => {
    expect(decodePreviewForm(new URLSearchParams('country=GR')).country).toBe(
      'GR'
    );
  });
});
