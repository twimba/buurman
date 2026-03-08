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
  propertyRegionCode?: string;
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
  success: boolean;
  previousRentAmount: number;
  newRentAmount: number;
  effectiveDate: string;
  paymentsCancelled: number;
  paymentsGenerated: number;
  error?: string;
}

export interface RentIncreaseSummary {
  totalContractsUpdated: number;
  totalRentPeriodsCreated: number;
  totalPaymentsCancelled: number;
  totalPaymentsGenerated: number;
  totalFailed: number;
}
