import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as contractsApi from '../api/contracts';
import {
  CreateContractRequest,
  UpdateContractRequest,
  ChangeContractStatusRequest,
  AddContractPartyRequest,
  ChangePrimaryContactRequest,
} from '../types/contract';
import { GetContractsParams } from '../api/contracts';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useContracts = (params?: GetContractsParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.contracts.all(params),
    queryFn: () => contractsApi.getContracts(params),
    placeholderData: keepPreviousData,
  });
};

export const useContract = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.detail(id),
    queryFn: () => contractsApi.getContract(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateContract = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContractRequest) =>
      contractsApi.createContract(data),
    onSuccess: (newContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newContract.property.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          newContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contract created successfully', 'success');
      trackEvent(AnalyticsEvent.CONTRACT_CREATED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContract = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateContractRequest) =>
      contractsApi.updateContract(id, data),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      // Contract changes may affect payment display
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      showToast('Contract updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContract = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => contractsApi.deleteContract(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contract deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useChangeContractStatus = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: ChangeContractStatusRequest) =>
      contractsApi.changeContractStatus(id, data),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      trackEvent(AnalyticsEvent.CONTRACT_STATUS_CHANGED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useReopenContract = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => contractsApi.reopenContract(id),
    onSuccess: (updatedContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedContract.property.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          updatedContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      // Reopening cancels future payments
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(id),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      showToast('Contract reopened successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDuplicateContract = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => contractsApi.duplicateContract(id),
    onSuccess: (newContract) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newContract.property.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(
          newContract.primaryContact?.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contract duplicated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContractDocuments = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.documents(contractId),
    queryFn: () => contractsApi.getContractDocuments(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useUploadContractDocument = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => contractsApi.uploadContractDocument(contractId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContractDocument = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      contractsApi.deleteContractDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContractAuditLog = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contracts.auditLog(contractId),
    queryFn: () => contractsApi.getContractAuditLog(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useGenerateContractPayments = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      count,
      markAsPaid,
    }: {
      count: number;
      markAsPaid?: boolean;
    }) =>
      contractsApi.generateContractPayments(contractId, { count, markAsPaid }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      const paidSuffix =
        result.markedAsPaid && result.markedAsPaid > 0
          ? ' and marked as paid'
          : '';
      trackEvent(AnalyticsEvent.PAYMENTS_GENERATED, {
        count: result.generated,
      });
      if (result.generated === result.requested) {
        showToast(
          `Scheduled ${result.generated} payment(s)${paidSuffix} successfully`,
          'success'
        );
      } else {
        showToast(
          `Scheduled ${result.generated} of ${result.requested} payment(s)${paidSuffix}. Some dates already had payments.`,
          'success'
        );
      }
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Country Metadata Schema hook ---

export const useContractMetadataSchema = (countryCode?: string) => {
  return useQuery({
    queryKey: queryKeys.contracts.metadataSchema(countryCode),
    queryFn: () => contractsApi.getContractMetadataSchema(countryCode ?? ''),
    enabled: !!countryCode,
    staleTime: Infinity, // Schemas don't change during a session
  });
};

// --- Contract Party hooks ---

export const useAddContractParty = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: AddContractPartyRequest) =>
      contractsApi.addContractParty(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      showToast('Party added successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useRemoveContractParty = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (partyIdentifier: string) =>
      contractsApi.removeContractParty(contractId, partyIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      showToast('Party removed successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useChangePrimaryContact = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: ChangePrimaryContactRequest) =>
      contractsApi.changePrimaryContact(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      showToast('Primary contact changed successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
