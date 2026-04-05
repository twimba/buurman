import type { RentIncreaseCountrySummary } from '@/types/rentIncrease';
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation('contracts');
  const currentYear = new Date().getFullYear();
  const yearOptions = [currentYear, currentYear + 1];

  return (
    <div className="space-y-6">
      <div className="bg-surface-card rounded-lg border border-border-default p-6">
        <h3 className="text-lg font-semibold text-text-primary mb-4">
          {t('rentIncrease.steps.selectYear')}
        </h3>
        <div className="flex gap-3">
          {yearOptions.map((y) => (
            <button
              key={y}
              onClick={() => onYearChange(y)}
              className={`px-6 py-3 rounded-lg text-lg font-medium transition-colors ${
                year === y
                  ? 'bg-primary-500 text-white'
                  : 'bg-surface-inset text-text-secondary hover:bg-neutral-100'
              }`}
            >
              {y}
            </button>
          ))}
        </div>
      </div>

      {countrySummaries.length > 0 && (
        <div className="bg-surface-card rounded-lg border border-border-default p-6">
          <h3 className="text-lg font-semibold text-text-primary mb-4">
            {t('rentRegulations.title')}
          </h3>
          <div className="space-y-3">
            {countrySummaries.map((summary) => (
              <div
                key={summary.countryCode}
                className="flex items-center justify-between p-3 rounded-lg bg-surface-page dark:bg-surface-card"
              >
                <div className="flex items-center gap-3">
                  <span className="text-xl">
                    {countryCodeToFlag(summary.countryCode)}
                  </span>
                  <div>
                    <p className="font-medium text-text-primary">
                      {summary.countryName}
                    </p>
                    <p className="text-sm text-text-secondary">
                      {summary.contractCount}{' '}
                      {summary.contractCount === 1 ? 'contract' : 'contracts'}
                    </p>
                  </div>
                </div>
                {summary.hasRegulationData ? (
                  <span className="text-xs px-2 py-1 rounded-full bg-success-bg text-success-text">
                    {t('rentIncrease.regulationDataAvailable')}
                  </span>
                ) : (
                  <span className="text-xs px-2 py-1 rounded-full bg-warning-bg text-warning-text">
                    {t('rentRegulations.noData')}
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
          className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {t('rentIncrease.nextAdjustIncreases')}
        </button>
      </div>
    </div>
  );
};
