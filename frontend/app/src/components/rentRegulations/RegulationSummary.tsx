import { Clock, Globe, MapPin, Percent, ScrollText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import {
  RichTextDisplay,
  StatusBadge,
  type BadgeColorVariant,
} from '@buurman/ui';
import type { RentRegulationCountryDetailResponse } from '@/types/rentRegulation';
import { RegulationDisclaimer } from './StalenessWarning';

function countryCodeToFlag(code: string): string {
  return code
    .toUpperCase()
    .split('')
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join('');
}

/**
 * Formats a `format: date` (date-only, no time component) value such as
 * `effectiveFrom` for display. `new Date(isoDateString)` parses a date-only
 * ISO string as UTC midnight, so calling `toLocaleDateString` on it rolls the
 * displayed date back a day in any negative-UTC-offset timezone. Building the
 * `Date` from explicit local-time components instead sidesteps that: it never
 * touches UTC, so it can't shift.
 */
function formatCalendarDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

interface RegulationSummaryProps {
  country: RentRegulationCountryDetailResponse;
}

const TOPIC_ORDER = [
  'NOTICE_PERIOD',
  'TENANCY_DURATION',
  'DEPOSIT',
  'LEASE_FORM',
  'REGISTRATION',
  'FEES_AND_PENALTIES',
  'OTHER',
] as const;

const lateFeeColor: Record<string, BadgeColorVariant> = {
  ALLOWED: 'emerald',
  CAPPED: 'amber',
  INTEREST_ONLY: 'blue',
  FORBIDDEN: 'red',
  UNKNOWN: 'gray',
};

export const RegulationSummary = ({ country }: RegulationSummaryProps) => {
  const { t } = useTranslation('contracts');
  return (
    <div className="space-y-4">
      <RegulationDisclaimer />

      <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
        {/* Country header */}
        <div className="px-6 py-5 border-b border-border-default">
          <div className="flex items-center gap-4">
            <span className="text-4xl leading-none">
              {countryCodeToFlag(country.countryCode)}
            </span>
            <div className="flex-1 min-w-0">
              <h2 className="text-xl font-bold text-text-primary">
                {country.countryName}
              </h2>
              <div className="flex items-center gap-3 mt-1">
                {country.hasRegionalRegulations && (
                  <span className="inline-flex items-center gap-1 text-xs text-text-secondary">
                    <MapPin className="h-3 w-3" />
                    {t('rentRegulations.regionalVariations')}
                  </span>
                )}
                {country.lastReviewedAt && (
                  <span className="inline-flex items-center gap-1 text-xs text-text-muted">
                    <Clock className="h-3 w-3" />
                    {t('rentRegulations.reviewed', {
                      date: new Date(country.lastReviewedAt).toLocaleDateString(
                        undefined,
                        {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        }
                      ),
                    })}
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Late-fee regime */}
        {country.lateFeePolicy && country.lateFeePolicy !== 'UNKNOWN' && (
          <div className="px-6 py-5 border-b border-border-default">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-muted mb-3">
              <Percent className="h-3.5 w-3.5" />
              {t('rentRegulations.lateFees.title')}
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge
                label={t(
                  `rentRegulations.lateFees.policy.${country.lateFeePolicy}`
                )}
                color={lateFeeColor[country.lateFeePolicy] ?? 'gray'}
                shape="pill"
              />
              {country.lateFeePolicy === 'CAPPED' &&
                country.lateFeeMaxPercentage != null && (
                  <span className="text-sm text-text-primary">
                    {t('rentRegulations.lateFees.maxPercentage', {
                      percentage: country.lateFeeMaxPercentage,
                    })}
                  </span>
                )}
            </div>
            {country.lateFeeNotes && (
              <p className="text-sm text-text-secondary mt-2">
                {country.lateFeeNotes}
              </p>
            )}
            {country.formalNoticeDays != null && (
              <p className="text-sm text-text-primary mt-2">
                {t('rentRegulations.lateFees.noticeDays', {
                  count: country.formalNoticeDays,
                })}
              </p>
            )}
          </div>
        )}

        {/* Tenancy-law reference */}
        {country.tenancyRules != null && country.tenancyRules.length > 0 && (
          <div className="px-6 py-5 border-b border-border-default">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-muted mb-1">
              <ScrollText className="h-3.5 w-3.5" />
              {t('rentRegulations.tenancyRules.title')}
            </div>
            <p className="text-xs text-text-muted mb-3">
              {t('rentRegulations.tenancyRules.subtitle')}
            </p>
            {TOPIC_ORDER.filter((topic) =>
              country.tenancyRules?.some((r) => r.topic === topic)
            ).map((topic) => (
              <div key={topic} className="mb-4 last:mb-0">
                <div className="text-sm font-semibold text-text-primary mb-1.5">
                  {t(`rentRegulations.tenancyRules.topic.${topic}`)}
                </div>
                <ul className="space-y-1.5">
                  {country.tenancyRules
                    ?.filter((r) => r.topic === topic)
                    .map((rule) => (
                      <li key={rule.identifier} className="text-sm">
                        <span className="text-text-secondary">
                          {rule.label}
                        </span>
                        {': '}
                        <span className="text-text-primary font-medium">
                          {rule.value}
                        </span>
                        {rule.regionCode && (
                          <span className="text-text-muted">
                            {' '}
                            ({rule.regionCode})
                          </span>
                        )}
                        {rule.effectiveFrom && (
                          <span className="text-text-muted">
                            {' — '}
                            {t('rentRegulations.tenancyRules.effectiveFrom', {
                              date: formatCalendarDate(rule.effectiveFrom),
                            })}
                          </span>
                        )}
                        {rule.legalBasis && (
                          <span className="text-text-muted">
                            {' '}
                            [{rule.legalBasis}]
                          </span>
                        )}
                        {rule.sourceUrl && (
                          <>
                            {' '}
                            <a
                              href={rule.sourceUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-text-link underline"
                            >
                              {t('rentRegulations.tenancyRules.source')}
                            </a>
                          </>
                        )}
                      </li>
                    ))}
                </ul>
              </div>
            ))}
          </div>
        )}

        {/* Summary — rich text */}
        {country.summary && (
          <div className="px-6 py-5">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-muted mb-3">
              <Globe className="h-3.5 w-3.5" />
              {t('rentRegulations.overview')}
            </div>
            <RichTextDisplay
              content={country.summary}
              className="text-sm text-text-secondary leading-relaxed prose-sm dark:prose-invert max-w-none"
            />
          </div>
        )}
      </div>
    </div>
  );
};
