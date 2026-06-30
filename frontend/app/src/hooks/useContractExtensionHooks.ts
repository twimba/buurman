import {
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listContractExtensions,
  getContractExtension,
  createContractExtension,
  activateContractExtension,
  confirmContractExtension,
  declineContractExtension,
  cancelContractExtension,
  generateExtensionDocuments,
} from '../generated/api/contract-extensions/contract-extensions';
import {
  getUpcomingRenewals,
  getPendingExtensions,
} from '../generated/api/dashboard/dashboard';
import { getJurisdictionDefaults } from '../generated/api/jurisdiction-defaults/jurisdiction-defaults';
import {
  CreateContractExtensionRequest,
  DeclineContractExtensionRequest,
} from '../types/contractExtension';
import type {
  GenerateExtensionDocumentsRequest,
  GetJurisdictionDefaultsParams,
  GetJurisdictionDefaultsLandlordType,
} from '../generated/models';
import { useToast } from '@buurman/ui';
import { queryKeys } from '../lib/queryKeys';

export const useContractExtensions = (
  contractId: string | undefined,
  page?: number,
  size?: number
) => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.all(contractId, page, size),
    queryFn: () => listContractExtensions(contractId ?? '', { page, size }),
    enabled: !!contractId,
    placeholderData: keepPreviousData,
  });
};

export const useContractExtension = (
  contractId: string | undefined,
  extensionId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.detail(contractId, extensionId),
    queryFn: () => getContractExtension(contractId ?? '', extensionId ?? ''),
    enabled: !!contractId && !!extensionId,
  });
};

export const useCreateExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Extension created successfully',
    mutationFn: (data: CreateContractExtensionRequest) =>
      createContractExtension(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.upcomingRenewals(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
    },
  });
};

export const useActivateExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Extension activated successfully',
    mutationFn: (extensionId: string) =>
      activateContractExtension(contractId, extensionId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.upcomingRenewals(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
    },
  });
};

export const useConfirmExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Extension confirmed successfully',
    mutationFn: (extensionId: string) =>
      confirmContractExtension(contractId, extensionId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.upcomingRenewals(),
      });
    },
  });
};

export const useDeclineExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Extension declined',
    mutationFn: ({
      extensionId,
      request,
    }: {
      extensionId: string;
      request: DeclineContractExtensionRequest;
    }) => declineContractExtension(contractId, extensionId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.upcomingRenewals(),
      });
    },
  });
};

export const useCancelExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Extension cancelled',
    mutationFn: ({
      extensionId,
      deleteDocuments,
    }: {
      extensionId: string;
      deleteDocuments?: boolean;
    }) => cancelContractExtension(contractId, extensionId, { deleteDocuments }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contractExtensions.upcomingRenewals(),
      });
      if (variables.deleteDocuments) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.contracts.documents(contractId),
        });
        queryClient.invalidateQueries({
          queryKey: queryKeys.documents.all(),
        });
      }
    },
  });
};

export const useGenerateExtensionDocuments = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: ({
      extensionId,
      request,
    }: {
      extensionId: string;
      request: GenerateExtensionDocumentsRequest;
    }) => generateExtensionDocuments(contractId, extensionId, request),
    onSuccess: (docs) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.documents.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      showToast(`${docs.length} document(s) generated successfully`, 'success');
    },
  });
};

export const useUpcomingRenewals = () => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.upcomingRenewals(),
    queryFn: () => getUpcomingRenewals(),
  });
};

export const usePendingExtensions = () => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.pendingExtensions(),
    queryFn: () => getPendingExtensions(),
  });
};

export const useJurisdictionDefaults = (
  countryCode: string | undefined,
  regionCode?: string,
  landlordType?: string,
  furnished?: boolean
) => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.jurisdictionDefaults(
      countryCode,
      regionCode,
      landlordType,
      furnished
    ),
    queryFn: () =>
      getJurisdictionDefaults({
        countryCode: countryCode ?? '',
        regionCode,
        landlordType: landlordType as
          GetJurisdictionDefaultsLandlordType | undefined,
        furnished,
      } as GetJurisdictionDefaultsParams),
    enabled: !!countryCode,
    staleTime: 1000 * 60 * 60,
  });
};
