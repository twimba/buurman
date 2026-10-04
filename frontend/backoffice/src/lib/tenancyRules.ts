import { TenancyRuleTopic } from '../generated/models';
import type { RentRegulationTenancyRuleResponse } from '../generated/models';

/** English headings for the backoffice (the backoffice has no i18n). */
export const TENANCY_TOPIC_LABELS: Record<TenancyRuleTopic, string> = {
  NOTICE_PERIOD: 'Notice period',
  TENANCY_DURATION: 'Tenancy duration',
  DEPOSIT: 'Deposit',
  LEASE_FORM: 'Lease form',
  REGISTRATION: 'Registration',
  FEES_AND_PENALTIES: 'Fees and penalties',
  OTHER: 'Other',
};

export interface TenancyRuleGroup {
  topic: TenancyRuleTopic;
  label: string;
  rules: RentRegulationTenancyRuleResponse[];
}

/** Groups rules by topic in enum order; topics without rules are left out. */
export const groupTenancyRules = (
  rules: RentRegulationTenancyRuleResponse[] | undefined
): TenancyRuleGroup[] =>
  Object.values(TenancyRuleTopic)
    .map((topic) => ({
      topic,
      label: TENANCY_TOPIC_LABELS[topic],
      rules: (rules ?? []).filter((r) => r.topic === topic),
    }))
    .filter((group) => group.rules.length > 0);

export const tenancyRuleScope = (
  rule: RentRegulationTenancyRuleResponse
): string => rule.regionCode ?? 'National';

/** The source link, only when it is a well-formed http(s) URL. */
export const tenancyRuleSourceHref = (
  rule: RentRegulationTenancyRuleResponse
): string | undefined => {
  if (!rule.sourceUrl) {
    return undefined;
  }
  try {
    const url = new URL(rule.sourceUrl);
    return url.protocol === 'https:' || url.protocol === 'http:'
      ? rule.sourceUrl
      : undefined;
  } catch {
    return undefined;
  }
};
