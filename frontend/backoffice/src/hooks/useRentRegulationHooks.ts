import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  rentRegulationsApi,
  type CreateCountryRequest,
  type UpdateCountryRequest,
  type CreateRegionRequest,
  type UpdateRegionRequest,
  type CreateRuleRequest,
  type UpdateRuleRequest,
  type BulkRuleRequest,
} from '../api/rentRegulations';

// ── Countries ────────────────────────────────────────────────────────

export const useRentRegulationCountries = () => {
  return useQuery({
    queryKey: ['rent-regulation-countries'],
    queryFn: () => rentRegulationsApi.listCountries().then((res) => res.data),
  });
};

export const useCreateCountry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCountryRequest) =>
      rentRegulationsApi.createCountry(data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useUpdateCountry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      code,
      data,
    }: {
      code: string;
      data: UpdateCountryRequest;
    }) => rentRegulationsApi.updateCountry(code, data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useDeleteCountry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => rentRegulationsApi.deleteCountry(code),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useReviewCountry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => rentRegulationsApi.reviewCountry(code),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

// ── Regions ──────────────────────────────────────────────────────────

export const useRentRegulationRegions = (countryCode: string) => {
  return useQuery({
    queryKey: ['rent-regulation-regions', countryCode],
    queryFn: () =>
      rentRegulationsApi.listRegions(countryCode).then((res) => res.data),
    enabled: !!countryCode,
  });
};

export const useCreateRegion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: CreateRegionRequest;
    }) =>
      rentRegulationsApi
        .createRegion(countryCode, data)
        .then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

export const useUpdateRegion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      countryCode,
      regionCode,
      data,
    }: {
      countryCode: string;
      regionCode: string;
      data: UpdateRegionRequest;
    }) =>
      rentRegulationsApi
        .updateRegion(countryCode, regionCode, data)
        .then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

export const useDeleteRegion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      countryCode,
      regionCode,
    }: {
      countryCode: string;
      regionCode: string;
    }) => rentRegulationsApi.deleteRegion(countryCode, regionCode),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

// ── Rules ────────────────────────────────────────────────────────────

export const useRentRegulationRules = (countryCode: string) => {
  return useQuery({
    queryKey: ['rent-regulation-rules', countryCode],
    queryFn: () =>
      rentRegulationsApi.listRules(countryCode).then((res) => res.data),
    enabled: !!countryCode,
  });
};

export const useCreateRule = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: CreateRuleRequest;
    }) =>
      rentRegulationsApi.createRule(countryCode, data).then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', variables.countryCode],
      });
    },
  });
};

export const useBulkCreateRules = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: BulkRuleRequest;
    }) =>
      rentRegulationsApi
        .bulkCreateRules(countryCode, data)
        .then((res) => res.data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', variables.countryCode],
      });
    },
  });
};

export const useUpdateRule = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateRuleRequest;
    }) =>
      rentRegulationsApi.updateRule(identifier, data).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', countryCode],
      });
    },
  });
};

export const useDeleteRule = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (identifier: string) =>
      rentRegulationsApi.deleteRule(identifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', countryCode],
      });
    },
  });
};

// ── Country Requests ────────────────────────────────────────────────

export const useCountryRegulationRequests = () => {
  return useQuery({
    queryKey: ['country-regulation-requests'],
    queryFn: () =>
      rentRegulationsApi.listCountryRequests().then((res) => res.data),
  });
};

export const useDismissCountryRequest = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (countryName: string) =>
      rentRegulationsApi.dismissCountryRequest(countryName),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['country-regulation-requests'],
      });
    },
  });
};
