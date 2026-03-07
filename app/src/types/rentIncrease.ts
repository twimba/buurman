import type { RentRegulationRuleResponse } from './rentRegulation';

export interface RentIncreasePreviewResponse {
  year: number;
  countrySummaries: RentIncreaseCountrySummary[];
  contracts: RentIncreaseContractPreview[];
}

export interface RentIncreaseCountrySummary {
  countryCode: string;
  countryName: string;
  contractCount: number;
  hasRegulationData: boolean;
  rules: RentRegulationRuleResponse[];
}

export interface RentIncreaseContractPreview {
  contractIdentifier: string;
  propertyIdentifier: string;
  propertyName: string;
  propertyAddress: string;
  propertyCountryCode: string;
  currentRentAmount: number;
  currency: string;
  regulationMinPercent?: number;
  regulationMaxPercent?: number;
  suggestedEffectiveDate?: string;
}

export interface ApplyRentIncreasesRequest {
  year: number;
  increases: RentIncreaseItem[];
}

export interface RentIncreaseItem {
  contractIdentifier: string;
  increasePercentage: number;
  newRentAmount: number;
  effectiveDate: string;
}

export interface ApplyRentIncreasesResponse {
  results: RentIncreaseResult[];
  summary: RentIncreaseSummary;
}

export interface RentIncreaseResult {
  contractIdentifier: string;
  propertyName: string;
  previousRentAmount: number;
  newRentAmount: number;
  increasePercentage: number;
  currency: string;
  effectiveDate: string;
  success: boolean;
  errorMessage?: string;
}

export interface RentIncreaseSummary {
  totalContracts: number;
  successCount: number;
  failureCount: number;
  totalsByCurrency: Record<string, CurrencyTotals>;
}

export interface CurrencyTotals {
  previousTotal: number;
  newTotal: number;
  increaseTotal: number;
}
