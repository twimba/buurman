import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getOnboardingStatus,
  completeOnboarding,
} from '../generated/api/onboarding/onboarding';
import {
  getCountryCurrencies,
  changeCurrency,
} from '../generated/api/currency/currency';
import type {
  CompleteOnboardingRequest,
  CurrencyChangeRequest,
} from '../types/onboarding';
import { useAuth } from '../context/AuthContext';
import { queryKeys } from '../lib/queryKeys';

export const useOnboardingStatus = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: queryKeys.onboarding.status(),
    queryFn: getOnboardingStatus,
    enabled: isAuthenticated,
    staleTime: 30_000,
  });
};

export const useCompleteOnboarding = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (request: CompleteOnboardingRequest) =>
      completeOnboarding(request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.onboarding.status(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.settings() });
    },
  });
};

export const useCountryCurrencies = () => {
  return useQuery({
    queryKey: queryKeys.onboarding.countryCurrencies(),
    queryFn: getCountryCurrencies,
    staleTime: Infinity,
  });
};

export const useChangeCurrency = (teamIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (request: CurrencyChangeRequest) =>
      changeCurrency(teamIdentifier, request),
    onSuccess: () => {
      // Currency change affects all financial data across the entire app
      queryClient.invalidateQueries();
    },
  });
};
