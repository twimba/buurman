import client from './client';

export interface OnboardingStatusResponse {
  completed: boolean;
  completedAt: string | null;
  currentCurrency: string;
  currentCountry: string;
}

export interface CompleteOnboardingRequest {
  country: string;
  currency: string;
  dateFormat?: string;
}

export interface CurrencyChangeRequest {
  newCurrency: string;
  mode: 'RELABEL' | 'CONVERT';
  conversionRate?: number;
}

export interface CurrencyChangeResponse {
  oldCurrency: string;
  newCurrency: string;
  mode: string;
  conversionRate: number | null;
  affectedContracts: number;
  affectedPayments: number;
  affectedExpenses: number;
  affectedFinancials: number;
}

export const getOnboardingStatus = async (): Promise<OnboardingStatusResponse> => {
  const { data } = await client.get('/onboarding/status');
  return data;
};

export const completeOnboarding = async (
  request: CompleteOnboardingRequest
): Promise<OnboardingStatusResponse> => {
  const { data } = await client.post('/onboarding/complete', request);
  return data;
};

export const getCountryCurrencies = async (): Promise<Record<string, string>> => {
  const { data } = await client.get('/countries/currencies');
  return data;
};

export const changeCurrency = async (
  teamIdentifier: string,
  request: CurrencyChangeRequest
): Promise<CurrencyChangeResponse> => {
  const { data } = await client.post(
    `/teams/${teamIdentifier}/currency-change`,
    request
  );
  return data;
};
