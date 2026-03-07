import type { RentIncreaseCountrySummary } from '@/types/rentIncrease';

function countryCodeToFlag(code: string): string {
  return code
    .toUpperCase()
    .split('')
    .map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65))
    .join('');
}

interface YearSelectionStepProps {
  year: number;
  onYearChange: (year: number) => void;
  countrySummaries: RentIncreaseCountrySummary[];
  onNext: () => void;
  isLoading: boolean;
}

export const YearSelectionStep = ({
  year,
  onYearChange,
  countrySummaries,
  onNext,
  isLoading,
}: YearSelectionStepProps) => {
  const currentYear = new Date().getFullYear();
  const yearOptions = [currentYear, currentYear + 1];

  return (
    <div className="space-y-6">
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
        <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
          Select Year
        </h3>
        <div className="flex gap-3">
          {yearOptions.map((y) => (
            <button
              key={y}
              onClick={() => onYearChange(y)}
              className={`px-6 py-3 rounded-lg text-lg font-medium transition-colors ${
                year === y
                  ? 'bg-[#5c7cfa] text-white'
                  : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
              }`}
            >
              {y}
            </button>
          ))}
        </div>
      </div>

      {countrySummaries.length > 0 && (
        <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-6">
          <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-4">
            Regulation Summary
          </h3>
          <div className="space-y-3">
            {countrySummaries.map((summary) => (
              <div
                key={summary.countryCode}
                className="flex items-center justify-between p-3 rounded-lg bg-[#f8f9fc] dark:bg-[#1a1c28]"
              >
                <div className="flex items-center gap-3">
                  <span className="text-xl">
                    {countryCodeToFlag(summary.countryCode)}
                  </span>
                  <div>
                    <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                      {summary.countryName}
                    </p>
                    <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                      {summary.contractCount}{' '}
                      {summary.contractCount === 1 ? 'contract' : 'contracts'}
                    </p>
                  </div>
                </div>
                {summary.hasRegulationData ? (
                  <span className="text-xs px-2 py-1 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400">
                    Regulation data available
                  </span>
                ) : (
                  <span className="text-xs px-2 py-1 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400">
                    No regulation data
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex justify-end">
        <button
          onClick={onNext}
          disabled={isLoading || countrySummaries.length === 0}
          className="bg-[#5c7cfa] text-white px-6 py-2 rounded hover:bg-[#4c6ef5] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Next: Adjust Increases
        </button>
      </div>
    </div>
  );
};
