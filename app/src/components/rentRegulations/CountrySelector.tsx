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
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {countries.map((country) => (
        <button
          key={country.countryCode}
          onClick={() => onSelect(country.countryCode)}
          className={`text-left p-4 rounded-lg border transition-all duration-200 ${
            selectedCode === country.countryCode
              ? 'border-[#5c7cfa] dark:border-[#748ffc] bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 ring-1 ring-[#5c7cfa]'
              : 'border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] hover:border-[#5c7cfa]/50 dark:hover:border-[#748ffc]/50'
          }`}
        >
          <div className="flex items-center gap-3 mb-2">
            <span className="text-2xl">{countryCodeToFlag(country.countryCode)}</span>
            <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
              {country.countryName}
            </span>
          </div>
          {country.summary && (
            <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] line-clamp-2">
              {country.summary}
            </p>
          )}
          <div className="mt-2 flex items-center gap-2">
            {country.hasRegionalRegulations && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8]">
                Regional rules
              </span>
            )}
            {country.stale && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400">
                Needs review
              </span>
            )}
          </div>
        </button>
      ))}
    </div>
  );
};
