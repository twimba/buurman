import client from "./client";

// ── Types ────────────────────────────────────────────────────────────

export interface RentRegulationCountryResponse {
  identifier: string;
  countryCode: string;
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
  lastReviewedAt?: string;
  stale: boolean;
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

export interface BulkImportResult {
  imported: number;
  errors: string[];
}

export interface CreateCountryRequest {
  countryCode: string;
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
}

export interface UpdateCountryRequest {
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
}

export interface CreateRegionRequest {
  regionCode: string;
  regionName: string;
  summary?: string;
}

export interface UpdateRegionRequest {
  regionName: string;
  summary?: string;
}

export interface CreateRuleRequest {
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

export type UpdateRuleRequest = CreateRuleRequest;

export interface BulkRuleRequest {
  rules: CreateRuleRequest[];
}

export interface CountryRegulationRequester {
  userIdentifier: string;
  userName: string;
  teamIdentifier: string;
  teamName: string;
  notes?: string;
  requestedAt: string;
}

export interface CountryRegulationRequestSummary {
  countryName: string;
  requestCount: number;
  firstRequestedAt: string;
  lastRequestedAt: string;
  requesters: CountryRegulationRequester[];
}

// ── API ──────────────────────────────────────────────────────────────

const BASE = "/rent-regulations";

export const rentRegulationsApi = {
  // Countries
  listCountries: () =>
    client.get<RentRegulationCountryResponse[]>(`${BASE}/countries`),
  createCountry: (data: CreateCountryRequest) =>
    client.post<RentRegulationCountryResponse>(`${BASE}/countries`, data),
  updateCountry: (code: string, data: UpdateCountryRequest) =>
    client.put<RentRegulationCountryResponse>(
      `${BASE}/countries/${code}`,
      data,
    ),
  deleteCountry: (code: string) => client.delete(`${BASE}/countries/${code}`),
  reviewCountry: (code: string) =>
    client.post(`${BASE}/countries/${code}/review`),

  // Regions
  listRegions: (countryCode: string) =>
    client.get<RentRegulationRegionResponse[]>(
      `${BASE}/countries/${countryCode}/regions`,
    ),
  createRegion: (countryCode: string, data: CreateRegionRequest) =>
    client.post<RentRegulationRegionResponse>(
      `${BASE}/countries/${countryCode}/regions`,
      data,
    ),
  updateRegion: (
    countryCode: string,
    regionCode: string,
    data: UpdateRegionRequest,
  ) =>
    client.put<RentRegulationRegionResponse>(
      `${BASE}/countries/${countryCode}/regions/${regionCode}`,
      data,
    ),
  deleteRegion: (countryCode: string, regionCode: string) =>
    client.delete(`${BASE}/countries/${countryCode}/regions/${regionCode}`),

  // Rules
  listRules: (countryCode: string) =>
    client.get<RentRegulationRuleResponse[]>(
      `${BASE}/countries/${countryCode}/rules`,
    ),
  createRule: (countryCode: string, data: CreateRuleRequest) =>
    client.post<RentRegulationRuleResponse>(
      `${BASE}/countries/${countryCode}/rules`,
      data,
    ),
  bulkCreateRules: (countryCode: string, data: BulkRuleRequest) =>
    client.post<BulkImportResult>(
      `${BASE}/countries/${countryCode}/rules/bulk`,
      data,
    ),
  updateRule: (identifier: string, data: UpdateRuleRequest) =>
    client.put<RentRegulationRuleResponse>(`${BASE}/rules/${identifier}`, data),
  deleteRule: (identifier: string) =>
    client.delete(`${BASE}/rules/${identifier}`),

  // Country requests
  listCountryRequests: () =>
    client.get<CountryRegulationRequestSummary[]>(`${BASE}/country-requests`),
  dismissCountryRequest: (countryName: string) =>
    client.delete(
      `${BASE}/country-requests/${encodeURIComponent(countryName)}`,
    ),
};
