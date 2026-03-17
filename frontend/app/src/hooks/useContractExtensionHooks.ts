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
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useContractExtensions = (
  contractId: string | undefined,
  page?: number,
  size?: number
) => {
  return useQuery({
    queryKey: ['contractExtensions', contractId, page, size],
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
    queryKey: ['contractExtension', contractId, extensionId],
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
        queryKey: ['contractExtensions', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['contractExtensions', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['contractExtensions', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
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
        queryKey: ['contractExtensions', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
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
    mutationFn: (extensionId: string) =>
      extensionsApi.cancelExtension(contractId, extensionId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['contractExtensions', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['upcomingRenewals'] });
      showToast('Extension cancelled', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpcomingRenewals = () => {
  return useQuery({
    queryKey: ['upcomingRenewals'],
    queryFn: () => extensionsApi.getUpcomingRenewals(),
  });
};

export const usePendingExtensions = () => {
  return useQuery({
    queryKey: ['pendingExtensions'],
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
    queryKey: [
      'jurisdictionDefaults',
      countryCode,
      regionCode,
      landlordType,
      furnished,
    ],
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
