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
                ? 'bg-white dark:bg-[#1e2130] text-[#1a1d2e] dark:text-[#eef0f6] shadow-sm ring-1 ring-[#5c7cfa] dark:ring-[#748ffc]'
                : 'bg-[#f8f9fc] dark:bg-[#0c0d14] text-[#6b7194] dark:text-[#8b90a8] hover:bg-white dark:hover:bg-[#1e2130] hover:text-[#3d4463] dark:hover:text-[#c4c8db]'
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
          className="group text-left p-5 rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f] hover:border-[#5c7cfa]/50 dark:hover:border-[#748ffc]/50 hover:shadow-md hover:shadow-[#5c7cfa]/5 transition-all duration-200"
        >
          <div className="flex items-center gap-3 mb-3">
            <span className="text-3xl leading-none">
              {countryCodeToFlag(country.countryCode)}
            </span>
            <div>
              <span className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6] group-hover:text-[#5c7cfa] dark:group-hover:text-[#91a7ff] transition-colors">
                {country.countryName}
              </span>
              {country.hasRegionalRegulations && (
                <span className="ml-2 text-xs px-2 py-0.5 rounded-full bg-[#f1f3f9] dark:bg-[#1e2130] text-[#6b7194] dark:text-[#8b90a8] ring-1 ring-[#e2e6f0] dark:ring-[#2a2e3f]">
                  Regional
                </span>
              )}
            </div>
          </div>
          {country.summary && (
            <div className="text-sm text-[#6b7194] dark:text-[#8b90a8] line-clamp-5">
              <RichTextDisplay
                content={country.summary}
                className="[&>*]:!m-0 [&>*]:!mb-1 [&>*]:text-sm [&>*]:text-[#6b7194] dark:[&>*]:text-[#8b90a8] [&>*]:leading-relaxed"
              />
            </div>
          )}
        </button>
      ))}
      <RequestCountryCard />
    </div>
  );
};
