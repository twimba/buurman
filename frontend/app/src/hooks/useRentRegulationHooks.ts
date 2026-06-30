import { useQuery } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listRentRegulationCountries,
  getRentRegulationCountryDetail,
  getCurrentRentRegulationRules,
  getRentRegulationRulesByYear,
  getCurrentRegionRentRegulationRules,
  requestCountryRegulation,
} from '../generated/api/rent-regulations/rent-regulations';
import { queryKeys } from '../lib/queryKeys';

export const useRentRegulationCountries = () => {
  return useQuery({
    queryKey: queryKeys.rentRegulation.countries(),
    queryFn: () => listRentRegulationCountries(),
  });
};

export const useRentRegulationCountryDetail = (code: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.rentRegulation.countryDetail(code),
    queryFn: () => getRentRegulationCountryDetail(code ?? ''),
    enabled: !!code,
  });
};

export const useRentRegulationCurrentRules = (code: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.rentRegulation.currentRules(code),
    queryFn: () => getCurrentRentRegulationRules(code ?? ''),
    enabled: !!code,
  });
};

export const useRentRegulationRulesByYear = (
  code: string | undefined,
  year: number | undefined
) => {
  return useQuery({
    queryKey: queryKeys.rentRegulation.rulesByYear(code, year),
    queryFn: () =>
      getRentRegulationRulesByYear(code ?? '', { year: year ?? 0 }),
    enabled: !!code && !!year,
  });
};

export const useRentRegulationRegionRules = (
  code: string | undefined,
  regionCode: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.rentRegulation.regionRules(code, regionCode),
    queryFn: () =>
      getCurrentRegionRentRegulationRules(code ?? '', regionCode ?? ''),
    enabled: !!code && !!regionCode,
  });
};

export const useRequestCountryRegulation = () => {
  return useMutationWithToast({
    successMessage: 'Your request has been submitted — thank you!',
    mutationFn: (data: { countryName: string; notes?: string }) =>
      requestCountryRegulation(data),
  });
};
