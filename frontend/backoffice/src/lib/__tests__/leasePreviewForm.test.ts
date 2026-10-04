import { describe, expect, it } from 'vitest';
import { DEFAULT_PREVIEW_FORM, buildPreviewRequest } from '../leasePreviewForm';
import type { PreviewForm } from '../leasePreviewForm';

const form = (patch: Partial<PreviewForm> = {}): PreviewForm => ({
  ...DEFAULT_PREVIEW_FORM,
  ...patch,
});

const errorsOf = (f: PreviewForm, choices = null as never) => {
  const r = buildPreviewRequest(f, choices);
  return r.ok ? [] : r.errors;
};

describe('buildPreviewRequest', () => {
  it('builds a valid request from the defaults', () => {
    const r = buildPreviewRequest(DEFAULT_PREVIEW_FORM, null);
    expect(r.ok).toBe(true);
    if (!r.ok) {
      return;
    }
    expect(r.request.countryCode).toBe('NL');
    expect(r.request.sample.contractType).toBe('FIXED_TERM');
    expect(r.request.sample.endDate).toBeDefined();
    expect(r.request.sample.rentComponents[0]).toMatchObject({
      type: 'BASE_RENT',
      currency: 'EUR',
    });
    expect(r.request.sample.deposit).toBeUndefined();
    expect(r.request.clauses).toBeUndefined();
  });

  it('omits endDate for an indefinite contract', () => {
    const r = buildPreviewRequest(form({ contractType: 'INDEFINITE' }), null);
    expect(r.ok && r.request.sample.endDate).toBeFalsy();
  });

  it('requires an end date after the start date for fixed term', () => {
    expect(errorsOf(form({ endDate: '' })).length).toBeGreaterThan(0);
    expect(
      errorsOf(form({ startDate: '2026-05-01', endDate: '2026-05-01' })).length
    ).toBeGreaterThan(0);
    expect(
      errorsOf(form({ startDate: '2026-05-01', endDate: '2026-04-01' })).length
    ).toBeGreaterThan(0);
  });

  it('ignores a bad end date when indefinite', () => {
    expect(errorsOf(form({ contractType: 'INDEFINITE', endDate: '' }))).toEqual(
      []
    );
  });

  it('validates amounts', () => {
    ['0', '-5', 'abc', '', '1000000001'].forEach((rentAmount) => {
      expect(errorsOf(form({ rentAmount })).length).toBeGreaterThan(0);
    });
    expect(errorsOf(form({ rentAmount: '1000000000' }))).toEqual([]);
  });

  it('adds optional utilities and deposit in the same currency', () => {
    const r = buildPreviewRequest(
      form({ utilitiesAmount: '90', depositAmount: '2400', currency: 'GBP' }),
      null
    );
    expect(r.ok).toBe(true);
    if (r.ok) {
      expect(r.request.sample.rentComponents).toHaveLength(2);
      expect(r.request.sample.rentComponents[1].type).toBe('UTILITIES_ADVANCE');
      expect(r.request.sample.deposit).toEqual({
        amount: 2400,
        currency: 'GBP',
      });
    }
    expect(errorsOf(form({ depositAmount: '0' })).length).toBeGreaterThan(0);
    expect(errorsOf(form({ utilitiesAmount: 'x' })).length).toBeGreaterThan(0);
  });

  it('requires an upper-case 3 letter currency', () => {
    expect(errorsOf(form({ currency: 'eur' })).length).toBeGreaterThan(0);
    expect(errorsOf(form({ currency: 'EU' })).length).toBeGreaterThan(0);
  });

  it('validates the payment day', () => {
    expect(errorsOf(form({ paymentDueDay: '0' })).length).toBeGreaterThan(0);
    expect(errorsOf(form({ paymentDueDay: '32' })).length).toBeGreaterThan(0);
    expect(errorsOf(form({ paymentDueDay: '1.5' })).length).toBeGreaterThan(0);
    const r = buildPreviewRequest(form({ paymentDueDay: '' }), null);
    expect(r.ok && r.request.sample.paymentDueDay).toBeUndefined();
  });

  it('validates text fields and tenants', () => {
    expect(errorsOf(form({ landlordName: '  ' })).length).toBeGreaterThan(0);
    expect(
      errorsOf(form({ propertyAddress: 'x'.repeat(201) })).length
    ).toBeGreaterThan(0);
    expect(errorsOf(form({ tenantNames: [] })).length).toBeGreaterThan(0);
    expect(errorsOf(form({ tenantNames: ['A', ''] })).length).toBeGreaterThan(
      0
    );
    expect(
      errorsOf(form({ tenantNames: Array(11).fill('A') })).length
    ).toBeGreaterThan(0);
    const r = buildPreviewRequest(
      form({ tenantNames: ['  Ann ', 'Bo'] }),
      null
    );
    expect(r.ok && r.request.sample.tenantNames).toEqual(['Ann', 'Bo']);
  });

  it('passes clause choices and rejects more than 100', () => {
    const choice = (i: number) => ({
      clauseKey: `c-${i}`,
      included: true,
      sortOrder: i,
    });
    const ok = buildPreviewRequest(form(), [choice(1), choice(2)]);
    expect(ok.ok && ok.request.clauses).toHaveLength(2);
    const many = Array.from({ length: 101 }, (_, i) => choice(i));
    expect(buildPreviewRequest(form(), many).ok).toBe(false);
  });
});
