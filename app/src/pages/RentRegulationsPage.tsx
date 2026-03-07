import { useState } from 'react';
import { Scale } from 'lucide-react';
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

export const RentRegulationsPage = () => {
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

  const displayRules =
    selectedRegion && regionRules ? regionRules : countryDetail?.rules;

  return (
    <div className="min-h-screen bg-background">
      <div className="px-4 py-8">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center gap-3 mb-1">
            <Scale className="h-8 w-8 text-primary-500 dark:text-primary-300" />
            <h1 className="text-3xl font-bold text-[#1a1d2e] dark:text-[#eef0f6]">
              Rent Regulations
            </h1>
          </div>
          <p className="text-[#6b7194] dark:text-[#8b90a8] ml-11">
            Reference data for rent increase rules by country
          </p>
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
          <div className="space-y-6">
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

                  <div>
                    <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6] mb-3">
                      {selectedRegion ? 'Regional Rules' : 'Regulation Rules'}
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
          <div className="flex flex-col items-center justify-center py-16 bg-white dark:bg-[#14161f] rounded-lg">
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
