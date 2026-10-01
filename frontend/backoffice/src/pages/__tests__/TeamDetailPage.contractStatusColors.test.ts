// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { contractStatusColors } from '../TeamDetailPage';

// Guards against NOTICE_GIVEN contracts rendering with the unstyled slate fallback
// (`colorMap[status] ?? 'bg-slate-100 text-slate-600'` in StatusDistribution) instead of a
// real color, matching the amber used by the app-side ContractStatusBadge for this status.

describe('TeamDetailPage contractStatusColors', () => {
  it('maps every known ContractStatus value, including NOTICE_GIVEN', () => {
    const knownStatuses = [
      'DRAFT',
      'PENDING_SIGNATURE',
      'ACTIVE',
      'EXPIRED',
      'TERMINATED',
      'NOTICE_GIVEN',
    ];

    for (const status of knownStatuses) {
      expect(contractStatusColors[status]).toBeDefined();
    }
  });

  it('styles NOTICE_GIVEN with amber, matching the app ContractStatusBadge color', () => {
    expect(contractStatusColors.NOTICE_GIVEN).toContain('amber');
  });

  it('does not fall back to the unstyled slate default used for unmapped statuses', () => {
    const unstyledFallback = 'bg-slate-100 text-slate-600';
    expect(contractStatusColors.NOTICE_GIVEN).not.toBe(unstyledFallback);
  });
});
