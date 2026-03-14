import { useQuery, useMutation } from '@tanstack/react-query';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';
import * as rentRegulationsApi from '../api/rentRegulations';

export const useRentRegulationCountries = () => {
  return useQuery({
    queryKey: ['rentRegulationCountries'],
    queryFn: rentRegulationsApi.getCountries,
  });
};

export const useRentRegulationCountryDetail = (code: string | undefined) => {
  return useQuery({
    queryKey: ['rentRegulationCountry', code],
    queryFn: () => rentRegulationsApi.getCountryDetail(code ?? ''),
    enabled: !!code,
  });
};

export const useRentRegulationCurrentRules = (code: string | undefined) => {
  return useQuery({
    queryKey: ['rentRegulationCurrentRules', code],
    queryFn: () => rentRegulationsApi.getCurrentRules(code ?? ''),
    enabled: !!code,
  });
};

export const useRentRegulationRulesByYear = (
  code: string | undefined,
  year: number | undefined
) => {
  return useQuery({
    queryKey: ['rentRegulationRules', code, year],
    queryFn: () => rentRegulationsApi.getRulesByYear(code ?? '', year ?? 0),
    enabled: !!code && !!year,
  });
};

export const useRentRegulationRegionRules = (
  code: string | undefined,
  regionCode: string | undefined
) => {
  return useQuery({
    queryKey: ['rentRegulationRegionRules', code, regionCode],
    queryFn: () =>
      rentRegulationsApi.getRegionCurrentRules(code ?? '', regionCode ?? ''),
    enabled: !!code && !!regionCode,
  });
};

export const useRequestCountryRegulation = () => {
  const { showToast } = useToast();

  return useMutation({
    mutationFn: (data: { countryName: string; notes?: string }) =>
      rentRegulationsApi.requestCountryRegulation(data),
    onSuccess: () => {
      showToast('Your request has been submitted — thank you!', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
