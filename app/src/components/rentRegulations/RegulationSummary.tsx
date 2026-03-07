import type { RentRegulationCountryDetailResponse } from '@/types/rentRegulation';
import { StalenessWarning } from './StalenessWarning';

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
      {country.stale && (
        <StalenessWarning lastReviewedAt={country.lastReviewedAt} />
      )}

      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <div className="flex items-center gap-3 mb-4">
          <span className="text-3xl">
            {countryCodeToFlag(country.countryCode)}
          </span>
          <div>
            <h2 className="text-xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              {country.countryName}
            </h2>
            {country.hasRegionalRegulations && (
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                Has regional regulation variations
              </p>
            )}
          </div>
        </div>

        {country.summary && (
          <p className="text-[#3d4463] dark:text-[#c4c8db] leading-relaxed">
            {country.summary}
          </p>
        )}

        {country.lastReviewedAt && (
          <p className="mt-4 text-xs text-[#9ca0b8] dark:text-[#5c6180]">
            Last reviewed:{' '}
            {new Date(country.lastReviewedAt).toLocaleDateString(undefined, {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })}
          </p>
        )}
      </div>
    </div>
  );
};
