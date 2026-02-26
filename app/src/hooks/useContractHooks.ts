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
  ChangePrimaryTenantRequest,
} from '../types/contract';
import { GetContractsParams } from '../api/contracts';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useContracts = (params?: GetContractsParams & PageParams) => {
  return useQuery({
    queryKey: ['contracts', params],
    queryFn: () => contractsApi.getContracts(params),
    placeholderData: keepPreviousData,
  });
};

export const useContract = (id: string | undefined) => {
  return useQuery({
    queryKey: ['contract', id],
    queryFn: () => contractsApi.getContract(id!),
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', newContract.property.identifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenants'],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', newContract.primaryTenant?.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Contract created successfully', 'success');
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['contract', id] });
      queryClient.invalidateQueries({ queryKey: ['contractAuditLog', id] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', updatedContract.property.identifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenants'],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.primaryTenant?.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      // Contract changes may affect payment display
      queryClient.invalidateQueries({ queryKey: ['paymentsByContract', id] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['contract', id] });
      queryClient.invalidateQueries({ queryKey: ['contractAuditLog', id] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', updatedContract.property.identifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.primaryTenant?.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      queryClient.invalidateQueries({ queryKey: ['paymentsByContract', id] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['contract', id] });
      queryClient.invalidateQueries({ queryKey: ['contractAuditLog', id] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', updatedContract.property.identifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.primaryTenant?.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      // Reopening cancels future payments
      queryClient.invalidateQueries({ queryKey: ['paymentsByContract', id] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
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
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['property', newContract.property.identifier],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', newContract.primaryTenant?.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Contract duplicated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContractDocuments = (contractId: string | undefined) => {
  return useQuery({
    queryKey: ['contractDocuments', contractId],
    queryFn: () => contractsApi.getContractDocuments(contractId!),
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
        queryKey: ['contractDocuments', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
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
        queryKey: ['contractDocuments', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContractAuditLog = (contractId: string | undefined) => {
  return useQuery({
    queryKey: ['contractAuditLog', contractId],
    queryFn: () => contractsApi.getContractAuditLog(contractId!),
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
        queryKey: ['paymentsByContract', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      const paidSuffix =
        result.markedAsPaid && result.markedAsPaid > 0
          ? ' and marked as paid'
          : '';
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
    queryKey: ['contract-metadata-schema', countryCode],
    queryFn: () => contractsApi.getContractMetadataSchema(countryCode!),
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
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
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
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      showToast('Party removed successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useChangePrimaryTenant = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: ChangePrimaryTenantRequest) =>
      contractsApi.changePrimaryTenant(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      showToast('Primary tenant changed successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
