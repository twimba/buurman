import { describe, expect, it } from 'vitest';
import { LeaseKind } from '../../generated/models';
import { LEASE_KIND_META, LEASE_KIND_ORDER } from '../leaseKindMeta';

describe('LEASE_KIND_META', () => {
  it('has an entry for every LeaseKind value', () => {
    expect(Object.keys(LEASE_KIND_META).sort()).toEqual(
      Object.values(LeaseKind).sort()
    );
  });

  it('keeps LEASE_KIND_ORDER equal to the enum order', () => {
    expect(LEASE_KIND_ORDER).toEqual(Object.values(LeaseKind));
  });

  it.each(Object.values(LeaseKind))('%s has non-empty copy', (kind) => {
    const meta = LEASE_KIND_META[kind];
    expect(meta.label.trim()).not.toBe('');
    expect(meta.oneLine.trim()).not.toBe('');
    expect(meta.appliesWhen.trim()).not.toBe('');
    expect(meta.fallbackNote.trim()).not.toBe('');
  });

  it('mirrors LeaseKind.fallbackChain() on the backend', () => {
    expect(LEASE_KIND_META.LEGACY.fallbackChain).toEqual(['LEGACY']);
    expect(LEASE_KIND_META.RESIDENTIAL_FURNISHED.fallbackChain).toEqual([
      'RESIDENTIAL_FURNISHED',
      'RESIDENTIAL',
      'LEGACY',
    ]);
    expect(LEASE_KIND_META.COMMERCIAL.fallbackChain).toEqual([
      'COMMERCIAL',
      'LEGACY',
    ]);
  });
});
