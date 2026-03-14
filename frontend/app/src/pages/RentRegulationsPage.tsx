import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Scale, TrendingUp, CalendarDays } from 'lucide-react';
import {
  useRentRegulationCountries,
  useRentRegulationCountryDetail,
  useRentRegulationRegionRules,
} from '@/hooks/useRentRegulationHooks';
import { CountrySelector } from '@/components/rentRegulations/CountrySelector';
import { RegulationSummary } from '@/components/rentRegulations/RegulationSummary';
import { RegionSelector } from '@/components/rentRegulations/RegionSelector';
import { RuleHistoryTable } from '@/components/rentRegulations/RuleHistoryTable';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorMessage } from '@/components/ErrorMessage';

const humanizeEnum = (value: string): string => {
  const labels: Record<string, string> = {
    FIXED_PERCENTAGE: 'Fixed Percentage',
    CPI_LINKED: 'CPI Linked',
    INDEX_LINKED: 'Index Linked',
    MARKET_RENT: 'Market Rent',
    NEGOTIATED: 'Negotiated',
    FROZEN: 'Frozen',
    OTHER: 'Other',
  };
  return (
    labels[value] ??
    value.replace(/_/g, '').replace(/\b\w/g, (c) => c.toUpperCase())
  );
};

export const RentRegulationsPage = () => {
  const navigate = useNavigate();
  const [selectedCountry, setSelectedCountry] = useState<string | undefined>();
  const [selectedRegion, setSelectedRegion] = useState<string | undefined>();

  const {
    data: countries,
    isLoading: countriesLoading,
    error: countriesError,
  } = useRentRegulationCountries();

  const { data: countryDetail, isLoading: detailLoading } =
    useRentRegulationCountryDetail(selectedCountry);

  const { data: regionRules } = useRentRegulationRegionRules(
    selectedCountry,
    selectedRegion
  );

  const handleCountrySelect = (code: string) => {
    setSelectedCountry(code);
    setSelectedRegion(undefined);
  };

  const displayRules =
    selectedRegion && regionRules ? regionRules : countryDetail?.rules;

  // Current year's rule (highlight card)
  const currentYear = new Date().getFullYear();
  const currentYearRule = useMemo(() => {
    if (!displayRules) {
      return null;
    }
    return displayRules.find((r) => r.year === currentYear) ?? null;
  }, [displayRules, currentYear]);

  if (countriesLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  if (countriesError) {
    return (
      <div className="min-h-screen bg-background p-8">
        <ErrorMessage message="Failed to load rent regulations" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Scale className="h-8 w-8 text-primary-500 dark:text-primary-300" />
              <h1 className="text-3xl font-bold text-text-primary">
                Rent Regulations
              </h1>
            </div>
            <p className="text-text-secondary ml-11">
              Reference data for rent adjustment rules by country
            </p>
          </div>
          <button
            onClick={() => navigate('/rent-increases/apply')}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-lg bg-gradient-to-b from-primary-500 to-primary-600 text-white border border-primary-600 shadow-sm shadow-primary-500/20 hover:from-primary-400 hover:to-primary-600 hover:shadow-md transition-all"
          >
            <TrendingUp className="h-4 w-4" />
            Apply Rent Adjustments
          </button>
        </div>

        {/* Country Selector */}
        {countries && (
          <div className="mb-6">
            <CountrySelector
              countries={countries}
              selectedCode={selectedCountry}
              onSelect={handleCountrySelect}
            />
          </div>
        )}

        {/* Country Detail */}
        {selectedCountry && (
          <div className="space-y-5">
            {detailLoading ? (
              <div className="flex justify-center py-12">
                <LoadingSpinner />
              </div>
            ) : (
              countryDetail && (
                <>
                  <RegulationSummary country={countryDetail} />

                  {countryDetail.hasRegionalRegulations && (
                    <RegionSelector
                      regions={countryDetail.regions}
                      selectedRegion={selectedRegion}
                      onSelect={setSelectedRegion}
                    />
                  )}

                  {/* Current year highlight */}
                  {currentYearRule && (
                    <div className="bg-gradient-to-r from-primary-50 to-purple-50 dark:from-primary-500/[0.08] dark:to-purple-500/[0.08] rounded-lg border border-primary-500/20 p-5">
                      <div className="flex items-center gap-2 mb-3">
                        <CalendarDays className="h-4 w-4 text-primary-500 dark:text-primary-300" />
                        <h3 className="text-sm font-semibold text-primary-500 dark:text-primary-300">
                          {currentYear} Current Rules
                        </h3>
                      </div>
                      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                        <div>
                          <p className="text-xs text-text-secondary mb-0.5">
                            Max Increase
                          </p>
                          <p className="text-lg font-bold text-text-primary">
                            {currentYearRule.maxIncreasePercentage != null
                              ? `${currentYearRule.maxIncreasePercentage}%`
                              : 'N/A'}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-text-secondary mb-0.5">
                            Type
                          </p>
                          <p className="text-sm font-semibold text-text-primary">
                            {humanizeEnum(currentYearRule.maxIncreaseType)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-text-secondary mb-0.5">
                            Effective Date
                          </p>
                          <p className="text-sm font-semibold text-text-primary">
                            {currentYearRule.effectiveDate ?? 'Not set'}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-text-secondary mb-0.5">
                            Notice Period
                          </p>
                          <p className="text-sm font-semibold text-text-primary">
                            {currentYearRule.noticePeriodDays != null
                              ? `${currentYearRule.noticePeriodDays} days`
                              : 'N/A'}
                          </p>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Rules table */}
                  <div>
                    <h3 className="text-lg font-semibold text-text-primary mb-3">
                      {selectedRegion ? 'Regional Rules' : 'Regulation History'}
                    </h3>
                    {displayRules && <RuleHistoryTable rules={displayRules} />}
                  </div>
                </>
              )
            )}
          </div>
        )}

        {/* Empty State */}
        {!selectedCountry && countries && countries.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 bg-surface-card rounded-lg border border-border-default">
            <Scale className="h-16 w-16 text-text-disabled mb-4" />
            <h3 className="text-lg font-semibold text-text-primary mb-2">
              No regulation data available
            </h3>
            <p className="text-text-secondary">
              Regulation data can be managed from the backoffice
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
