import { describe, expect, it } from 'vitest';
import { TenancyRuleTopic } from '../../generated/models';
import type { RentRegulationTenancyRuleResponse } from '../../generated/models';
import {
  TENANCY_TOPIC_LABELS,
  groupTenancyRules,
  tenancyRuleScope,
  tenancyRuleSourceHref,
} from '../tenancyRules';

const rule = (
  overrides: Partial<RentRegulationTenancyRuleResponse>
): RentRegulationTenancyRuleResponse => ({
  identifier: 'id',
  topic: TenancyRuleTopic.OTHER,
  label: 'label',
  value: 'value',
  ...overrides,
});

describe('TENANCY_TOPIC_LABELS', () => {
  it('labels every topic of the enum', () => {
    expect(Object.keys(TENANCY_TOPIC_LABELS).sort()).toEqual(
      Object.values(TenancyRuleTopic).sort()
    );
  });
});

describe('groupTenancyRules', () => {
  it('groups by topic in enum order and drops empty topics', () => {
    const groups = groupTenancyRules([
      rule({ identifier: 'a', topic: TenancyRuleTopic.OTHER }),
      rule({ identifier: 'b', topic: TenancyRuleTopic.DEPOSIT }),
      rule({ identifier: 'c', topic: TenancyRuleTopic.NOTICE_PERIOD }),
      rule({ identifier: 'd', topic: TenancyRuleTopic.NOTICE_PERIOD }),
    ]);

    expect(groups.map((g) => g.topic)).toEqual([
      TenancyRuleTopic.NOTICE_PERIOD,
      TenancyRuleTopic.DEPOSIT,
      TenancyRuleTopic.OTHER,
    ]);
    expect(groups[0].label).toBe('Notice period');
    expect(groups[0].rules.map((r) => r.identifier)).toEqual(['c', 'd']);
  });

  it('returns no groups for no rules', () => {
    expect(groupTenancyRules(undefined)).toEqual([]);
    expect(groupTenancyRules([])).toEqual([]);
  });
});

describe('tenancyRuleScope', () => {
  it('reads National when there is no region', () => {
    expect(tenancyRuleScope(rule({}))).toBe('National');
    expect(tenancyRuleScope(rule({ regionCode: null }))).toBe('National');
  });

  it('shows the region code otherwise', () => {
    expect(tenancyRuleScope(rule({ regionCode: 'ATT' }))).toBe('ATT');
  });
});

describe('tenancyRuleSourceHref', () => {
  it('keeps http(s) links', () => {
    expect(
      tenancyRuleSourceHref(rule({ sourceUrl: 'https://api.et.gr/x.pdf' }))
    ).toBe('https://api.et.gr/x.pdf');
  });

  it('rejects other schemes and missing links', () => {
    expect(
      tenancyRuleSourceHref(rule({ sourceUrl: 'javascript:alert(1)' }))
    ).toBeUndefined();
    expect(tenancyRuleSourceHref(rule({ sourceUrl: 'not a url' }))).toBe(
      undefined
    );
    expect(tenancyRuleSourceHref(rule({}))).toBeUndefined();
  });
});
