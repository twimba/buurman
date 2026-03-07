export interface RentRegulationCountryResponse {
  identifier: string;
  countryCode: string;
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
  lastReviewedAt?: string;
  stale: boolean;
}

export interface RentRegulationCountryDetailResponse extends RentRegulationCountryResponse {
  regions: RentRegulationRegionResponse[];
  rules: RentRegulationRuleResponse[];
}

export interface RentRegulationRegionResponse {
  identifier: string;
  regionCode: string;
  regionName: string;
  summary?: string;
}

export interface RentRegulationRuleResponse {
  identifier: string;
  year: number;
  propertyCategory: string;
  sector?: string;
  maxIncreasePercentage?: number;
  maxIncreaseType: string;
  indexName?: string;
  indexValue?: number;
  effectiveDate?: string;
  noticePeriodDays?: number;
  frequency: string;
  additionalConditions?: string;
  sourceUrl?: string;
  notes?: string;
}
