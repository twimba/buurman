import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getRentRegulationCatalogInfo,
  diffRentRegulationCatalog,
  reloadRentRegulationCatalog,
  listBackofficeRentRegulationCountries,
  createRentRegulationCountry,
  updateRentRegulationCountry,
  deleteRentRegulationCountry,
  reviewRentRegulationCountry,
  listRentRegulationRegions,
  createRentRegulationRegion,
  updateRentRegulationRegion,
  deleteRentRegulationRegion,
  listRentRegulationRules,
  listRentRegulationTenancyRules,
  createRentRegulationRule,
  bulkImportRentRegulationRules,
  updateRentRegulationRule,
  deleteRentRegulationRule,
  listCountryRegulationRequests,
  dismissCountryRegulationRequest,
} from '../generated/api/backoffice-rent-regulations/backoffice-rent-regulations';
import type {
  CreateRentRegulationCountryRequest,
  UpdateRentRegulationCountryRequest,
  CreateRentRegulationRegionRequest,
  UpdateRentRegulationRegionRequest,
  CreateRentRegulationRuleRequest,
  BulkCreateRentRegulationRulesRequest,
} from '../generated/models';
import type {
  CreateCountryRequest,
  UpdateCountryRequest,
  CreateRegionRequest,
  UpdateRegionRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  BulkRuleRequest,
} from '../types';

// ── Bundled catalog ──────────────────────────────────────────────────

export const useRentRegulationCatalogInfo = () => {
  return useQuery({
    queryKey: ['rent-regulation-catalog-info'],
    queryFn: () => getRentRegulationCatalogInfo(),
  });
};

export const useRentRegulationCatalogDiff = (enabled: boolean) => {
  return useQuery({
    queryKey: ['rent-regulation-catalog-diff'],
    queryFn: () => diffRentRegulationCatalog(),
    enabled,
    staleTime: 0,
  });
};

export const useReloadRentRegulationCatalog = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: () => reloadRentRegulationCatalog(),
    errorTitle: "Couldn't reload catalog",
    onSuccess: () => {
      // The reload wipes and re-seeds everything: invalidate all reference data.
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
      queryClient.invalidateQueries({ queryKey: ['rent-regulation-regions'] });
      queryClient.invalidateQueries({ queryKey: ['rent-regulation-rules'] });
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-tenancy-rules'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-catalog-diff'],
      });
    },
  });
};

// ── Countries ────────────────────────────────────────────────────────

export const useRentRegulationCountries = () => {
  return useQuery({
    queryKey: ['rent-regulation-countries'],
    queryFn: () => listBackofficeRentRegulationCountries(),
  });
};

export const useCreateCountry = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateCountryRequest) =>
      createRentRegulationCountry(data as CreateRentRegulationCountryRequest),
    errorTitle: "Couldn't create country",
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useUpdateCountry = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      code,
      data,
    }: {
      code: string;
      data: UpdateCountryRequest;
    }) =>
      updateRentRegulationCountry(
        code,
        data as UpdateRentRegulationCountryRequest
      ),
    errorTitle: "Couldn't update country",
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useDeleteCountry = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (code: string) => deleteRentRegulationCountry(code),
    errorTitle: "Couldn't delete country",
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-countries'],
      });
    },
  });
};

export const useReviewCountry = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (code: string) => reviewRentRegulationCountry(code),
    errorTitle: "Couldn't review country",
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
    queryFn: () => listRentRegulationRegions(countryCode),
    enabled: !!countryCode,
  });
};

export const useCreateRegion = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: CreateRegionRequest;
    }) =>
      createRentRegulationRegion(
        countryCode,
        data as CreateRentRegulationRegionRequest
      ),
    errorTitle: "Couldn't create region",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

export const useUpdateRegion = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      countryCode,
      regionCode,
      data,
    }: {
      countryCode: string;
      regionCode: string;
      data: UpdateRegionRequest;
    }) =>
      updateRentRegulationRegion(
        countryCode,
        regionCode,
        data as UpdateRentRegulationRegionRequest
      ),
    errorTitle: "Couldn't update region",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

export const useDeleteRegion = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      countryCode,
      regionCode,
    }: {
      countryCode: string;
      regionCode: string;
    }) => deleteRentRegulationRegion(countryCode, regionCode),
    errorTitle: "Couldn't delete region",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-regions', variables.countryCode],
      });
    },
  });
};

// ── Tenancy rules (display-only, seeded from the catalog) ────────────

export const useRentRegulationTenancyRules = (countryCode: string) => {
  return useQuery({
    queryKey: ['rent-regulation-tenancy-rules', countryCode],
    queryFn: () => listRentRegulationTenancyRules(countryCode),
    enabled: !!countryCode,
  });
};

// ── Rules ────────────────────────────────────────────────────────────

export const useRentRegulationRules = (countryCode: string) => {
  return useQuery({
    queryKey: ['rent-regulation-rules', countryCode],
    queryFn: () => listRentRegulationRules(countryCode),
    enabled: !!countryCode,
  });
};

export const useCreateRule = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: CreateRuleRequest;
    }) =>
      createRentRegulationRule(
        countryCode,
        data as CreateRentRegulationRuleRequest
      ),
    errorTitle: "Couldn't create rule",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', variables.countryCode],
      });
    },
  });
};

export const useBulkCreateRules = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      countryCode,
      data,
    }: {
      countryCode: string;
      data: BulkRuleRequest;
    }) =>
      bulkImportRentRegulationRules(
        countryCode,
        data as BulkCreateRentRegulationRulesRequest
      ),
    errorTitle: "Couldn't import rules",
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', variables.countryCode],
      });
    },
  });
};

export const useUpdateRule = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      identifier,
      data,
    }: {
      identifier: string;
      data: UpdateRuleRequest;
    }) =>
      updateRentRegulationRule(
        identifier,
        data as CreateRentRegulationRuleRequest
      ),
    errorTitle: "Couldn't update rule",
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['rent-regulation-rules', countryCode],
      });
    },
  });
};

export const useDeleteRule = (countryCode: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (identifier: string) => deleteRentRegulationRule(identifier),
    errorTitle: "Couldn't delete rule",
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
    queryFn: () => listCountryRegulationRequests(),
  });
};

export const useDismissCountryRequest = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (countryName: string) =>
      dismissCountryRegulationRequest(countryName),
    errorTitle: "Couldn't dismiss request",
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['country-regulation-requests'],
      });
    },
  });
};
