import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as extensionsApi from '../api/contractExtensions';
import {
  CreateContractExtensionRequest,
  DeclineContractExtensionRequest,
} from '../types/contractExtension';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useContractExtensions = (
  contractId: string | undefined,
  page?: number,
  size?: number
) => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.all(contractId, page, size),
    queryFn: () => extensionsApi.listExtensions(contractId ?? '', page, size),
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
    queryFn: () =>
      extensionsApi.getExtension(contractId ?? '', extensionId ?? ''),
    enabled: !!contractId && !!extensionId,
  });
};

export const useCreateExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContractExtensionRequest) =>
      extensionsApi.createExtension(contractId, data),
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
      showToast('Extension created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useActivateExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (extensionId: string) =>
      extensionsApi.activateExtension(contractId, extensionId),
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
      showToast('Extension activated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useConfirmExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (extensionId: string) =>
      extensionsApi.confirmExtension(contractId, extensionId),
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
      showToast('Extension confirmed successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeclineExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      extensionId,
      request,
    }: {
      extensionId: string;
      request: DeclineContractExtensionRequest;
    }) => extensionsApi.declineExtension(contractId, extensionId, request),
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
      showToast('Extension declined', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useCancelExtension = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      extensionId,
      deleteDocuments,
    }: {
      extensionId: string;
      deleteDocuments?: boolean;
    }) =>
      extensionsApi.cancelExtension(contractId, extensionId, deleteDocuments),
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
      showToast('Extension cancelled', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useGenerateExtensionDocuments = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      extensionId,
      request,
    }: {
      extensionId: string;
      request: extensionsApi.GenerateExtensionDocumentsRequest;
    }) =>
      extensionsApi.generateExtensionDocuments(
        contractId,
        extensionId,
        request
      ),
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
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpcomingRenewals = () => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.upcomingRenewals(),
    queryFn: () => extensionsApi.getUpcomingRenewals(),
  });
};

export const usePendingExtensions = () => {
  return useQuery({
    queryKey: queryKeys.contractExtensions.pendingExtensions(),
    queryFn: () => extensionsApi.getPendingExtensions(),
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
      extensionsApi.getJurisdictionDefaults(
        countryCode ?? '',
        regionCode,
        landlordType,
        furnished
      ),
    enabled: !!countryCode,
    staleTime: 1000 * 60 * 60,
  });
};
