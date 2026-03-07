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
    value.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
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
              <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                Rent Regulations
              </h1>
            </div>
            <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
              Reference data for rent adjustment rules by country
            </p>
          </div>
          <button
            onClick={() => navigate('/rent-increases/apply')}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-lg bg-gradient-to-b from-[#5c7cfa] to-[#4c6ef5] text-white border border-[#4263eb] shadow-sm shadow-[#5c7cfa]/20 hover:from-[#4c6ef5] hover:to-[#4263eb] hover:shadow-md transition-all"
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
                    <div className="bg-gradient-to-r from-[#f0f4ff] to-[#f5f0ff] dark:from-[#5c7cfa]/[0.08] dark:to-[#845ef7]/[0.08] rounded-xl border border-[#5c7cfa]/20 dark:border-[#5c7cfa]/15 p-5">
                      <div className="flex items-center gap-2 mb-3">
                        <CalendarDays className="h-4 w-4 text-[#5c7cfa] dark:text-[#91a7ff]" />
                        <h3 className="text-sm font-semibold text-[#5c7cfa] dark:text-[#91a7ff]">
                          {currentYear} Current Rules
                        </h3>
                      </div>
                      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                        <div>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-0.5">
                            Max Increase
                          </p>
                          <p className="text-lg font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
                            {currentYearRule.maxIncreasePercentage != null
                              ? `${currentYearRule.maxIncreasePercentage}%`
                              : 'N/A'}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-0.5">
                            Type
                          </p>
                          <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                            {humanizeEnum(currentYearRule.maxIncreaseType)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-0.5">
                            Effective Date
                          </p>
                          <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                            {currentYearRule.effectiveDate ?? 'Not set'}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mb-0.5">
                            Notice Period
                          </p>
                          <p className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
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
                    <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
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
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-[#14161f] rounded-xl border border-[#e2e6f0] dark:border-[#2a2e3f]">
            <Scale className="h-16 w-16 text-[#c9cfd9] dark:text-[#3a3f54] mb-4" />
            <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-2">
              No regulation data available
            </h3>
            <p className="text-[#6b7194] dark:text-[#8b90a8]">
              Regulation data can be managed from the backoffice
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
