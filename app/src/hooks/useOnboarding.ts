import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getOnboardingStatus,
  completeOnboarding,
  getCountryCurrencies,
  changeCurrency,
  CompleteOnboardingRequest,
  CurrencyChangeRequest,
} from '../api/onboarding';
import { useAuth } from '../contexts/AuthContext';

export const useOnboardingStatus = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['onboarding-status'],
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
      queryClient.invalidateQueries({ queryKey: ['onboarding-status'] });
      queryClient.invalidateQueries({ queryKey: ['team-settings'] });
    },
  });
};

export const useCountryCurrencies = () => {
  return useQuery({
    queryKey: ['country-currencies'],
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
      queryClient.invalidateQueries({ queryKey: ['team-settings'] });
      queryClient.invalidateQueries({ queryKey: ['onboarding-status'] });
    },
  });
};
