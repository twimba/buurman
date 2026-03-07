import { Clock, Globe, MapPin } from 'lucide-react';
import { RichTextDisplay } from '@/components/common/RichTextDisplay';
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

export const RegulationSummary = ({ country }: RegulationSummaryProps) => {
  return (
    <div className="space-y-4">
      <RegulationDisclaimer />

      <div className="bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
        {/* Country header */}
        <div className="px-6 py-5 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center gap-4">
            <span className="text-4xl leading-none">
              {countryCodeToFlag(country.countryCode)}
            </span>
            <div className="flex-1 min-w-0">
              <h2 className="text-xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                {country.countryName}
              </h2>
              <div className="flex items-center gap-3 mt-1">
                {country.hasRegionalRegulations && (
                  <span className="inline-flex items-center gap-1 text-xs text-[#6b7194] dark:text-[#8b90a8]">
                    <MapPin className="h-3 w-3" />
                    Regional variations
                  </span>
                )}
                {country.lastReviewedAt && (
                  <span className="inline-flex items-center gap-1 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                    <Clock className="h-3 w-3" />
                    Reviewed{' '}
                    {new Date(country.lastReviewedAt).toLocaleDateString(
                      undefined,
                      {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                      }
                    )}
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Summary — rich text */}
        {country.summary && (
          <div className="px-6 py-5">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-3">
              <Globe className="h-3.5 w-3.5" />
              Overview
            </div>
            <RichTextDisplay
              content={country.summary}
              className="text-sm text-[#3d4463] dark:text-[#c4c8db] leading-relaxed prose-sm dark:prose-invert max-w-none"
            />
          </div>
        )}
      </div>
    </div>
  );
};
