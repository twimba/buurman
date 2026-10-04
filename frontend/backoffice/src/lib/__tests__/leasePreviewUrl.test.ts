import { describe, expect, it } from 'vitest';
import { DEFAULT_PREVIEW_FORM } from '../leasePreviewForm';
import type { PreviewForm } from '../leasePreviewForm';
import { decodePreviewForm, encodePreviewForm } from '../leasePreviewUrl';

const roundTrip = (f: PreviewForm) =>
  decodePreviewForm(new URLSearchParams(encodePreviewForm(f).toString()));

describe('preview URL state', () => {
  it('encodes the defaults as an empty query and decodes it back', () => {
    expect(encodePreviewForm(DEFAULT_PREVIEW_FORM).toString()).toBe('');
    expect(decodePreviewForm(new URLSearchParams())).toEqual(
      DEFAULT_PREVIEW_FORM
    );
  });

  it('round-trips a customised form', () => {
    const f: PreviewForm = {
      country: 'DE',
      kind: 'RESIDENTIAL_FURNISHED',
      language: 'de',
      contractType: 'INDEFINITE',
      startDate: '2027-02-01',
      endDate: '',
      rentAmount: '1250.50',
      utilitiesAmount: '80',
      currency: 'GBP',
      depositAmount: '2500',
      paymentDueDay: '5',
      landlordName: 'A & B = "C"',
      tenantNames: ['Ann', 'Bo ü', ''],
      propertyAddress: 'Straße 1, Berlin',
    };
    expect(roundTrip(f)).toEqual(f);
  });

  it('keeps explicitly emptied text fields instead of the default', () => {
    const f = { ...DEFAULT_PREVIEW_FORM, landlordName: '', depositAmount: '' };
    expect(roundTrip(f).landlordName).toBe('');
  });

  it('falls back to defaults for invalid enum values and unknown countries', () => {
    const d = decodePreviewForm(
      new URLSearchParams('country=nl1&kind=NOPE&lang=xx&type=WEIRD')
    );
    expect(d).toEqual(DEFAULT_PREVIEW_FORM);
    expect(decodePreviewForm(new URLSearchParams('country=XX')).country).toBe(
      DEFAULT_PREVIEW_FORM.country
    );
    expect(decodePreviewForm(new URLSearchParams('country=DE')).country).toBe(
      'DE'
    );
    expect(decodePreviewForm(new URLSearchParams('kind=LEGACY')).kind).toBe(
      'LEGACY'
    );
  });

  it('keeps partially typed values instead of resetting them', () => {
    const typed = (patch: Partial<PreviewForm>) =>
      roundTrip({ ...DEFAULT_PREVIEW_FORM, ...patch });
    expect(typed({ currency: 'G' }).currency).toBe('G');
    expect(typed({ currency: 'EU' }).currency).toBe('EU');
    expect(typed({ paymentDueDay: '' }).paymentDueDay).toBe('');
    expect(typed({ paymentDueDay: '0' }).paymentDueDay).toBe('0');
    expect(typed({ rentAmount: '1,5' }).rentAmount).toBe('1,5');
    expect(typed({ startDate: '' }).startDate).toBe('');
  });

  it('caps tenants at 3 and truncates long text', () => {
    const d = decodePreviewForm(
      new URLSearchParams(
        'tenant=a&tenant=b&tenant=c&tenant=d&landlord=' + 'x'.repeat(500)
      )
    );
    expect(d.tenantNames).toEqual(['a', 'b', 'c']);
    expect(d.landlordName).toHaveLength(200);
  });
});
