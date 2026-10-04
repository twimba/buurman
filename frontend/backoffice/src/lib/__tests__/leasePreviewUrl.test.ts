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

  it('falls back to defaults for invalid values', () => {
    const d = decodePreviewForm(
      new URLSearchParams(
        'country=nl1&kind=NOPE&lang=xx&type=WEIRD&start=tomorrow&end=2026-13-99&cur=eur&rent=abc&day=99&deposit=1e9'
      )
    );
    expect(d).toEqual(DEFAULT_PREVIEW_FORM);
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
