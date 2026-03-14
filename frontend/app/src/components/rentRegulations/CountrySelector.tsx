import { RichTextDisplay } from '@/components/common/RichTextDisplay';
import { RequestCountryCard } from './RequestCountryCard';
import type { RentRegulationCountryResponse } from '@/types/rentRegulation';

function countryCodeToFlag(code: string): string {
  return code
    .toUpperCase()
    .split('')
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join('');
}

interface CountrySelectorProps {
  countries: RentRegulationCountryResponse[];
  selectedCode?: string;
  onSelect: (code: string) => void;
}

export const CountrySelector = ({
  countries,
  selectedCode,
  onSelect,
}: CountrySelectorProps) => {
  // Compact horizontal strip when a country is selected
  if (selectedCode) {
    return (
      <div className="flex items-center gap-2 flex-wrap">
        {countries.map((country) => (
          <button
            key={country.countryCode}
            onClick={() => onSelect(country.countryCode)}
            className={`inline-flex items-center gap-2 px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-150 ${
              selectedCode === country.countryCode
                ? 'bg-surface-card text-text-primary shadow-sm ring-1 ring-primary-500 dark:ring-primary-400'
                : 'bg-surface-page text-text-secondary hover:bg-surface-card hover:text-text-secondary'
            }`}
          >
            <span className="text-lg leading-none">
              {countryCodeToFlag(country.countryCode)}
            </span>
            {country.countryName}
          </button>
        ))}
      </div>
    );
  }

  // Full cards when nothing is selected
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {countries.map((country) => (
        <button
          key={country.countryCode}
          onClick={() => onSelect(country.countryCode)}
          className="group text-left p-5 rounded-lg border border-border-default bg-surface-card hover:border-primary-500/50 dark:hover:border-primary-400/50 hover:shadow-md hover:shadow-primary-500/5 transition-all duration-200"
        >
          <div className="flex items-center gap-3 mb-3">
            <span className="text-3xl leading-none">
              {countryCodeToFlag(country.countryCode)}
            </span>
            <div>
              <span className="font-semibold text-text-primary group-hover:text-primary-500 transition-colors">
                {country.countryName}
              </span>
              {country.hasRegionalRegulations && (
                <span className="ml-2 text-xs px-2 py-0.5 rounded-full bg-surface-inset text-text-secondary ring-1 ring-border-default dark:ring-border-strong">
                  Regional
                </span>
              )}
            </div>
          </div>
          {country.summary && (
            <div className="text-sm text-text-secondary line-clamp-5">
              <RichTextDisplay
                content={country.summary}
                className="[&>*]:!m-0 [&>*]:!mb-1 [&>*]:text-sm [&>*]:text-text-muted dark:[&>*]:text-text-secondary [&>*]:leading-relaxed"
              />
            </div>
          )}
        </button>
      ))}
      <RequestCountryCard />
    </div>
  );
};
