import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getOnboardingStatus,
  completeOnboarding,
  getCountryCurrencies,
  changeCurrency,
  CompleteOnboardingRequest,
  CurrencyChangeRequest,
} from '../api/onboarding';
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
  return useMutation({
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
  return useMutation({
    mutationFn: (request: CurrencyChangeRequest) =>
      changeCurrency(teamIdentifier, request),
    onSuccess: () => {
      // Currency change affects all financial data across the entire app
      queryClient.invalidateQueries();
    },
  });
};
