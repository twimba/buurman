import { Clock, Globe, MapPin, Percent } from 'lucide-react';
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

interface RegulationSummaryProps {
  country: RentRegulationCountryDetailResponse;
}

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
