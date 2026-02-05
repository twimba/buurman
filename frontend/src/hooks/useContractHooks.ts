import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as contractsApi from '../api/contracts';
import {
  CreateContractRequest,
  UpdateContractRequest,
  ChangeContractStatusRequest,
} from '../types/contract';
import { GetContractsParams } from '../api/contracts';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useContracts = (params?: GetContractsParams) => {
  return useQuery({
    queryKey: ['contracts', params],
    queryFn: () => contractsApi.getContracts(params),
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
        queryKey: ['property', newContract.property.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenants'],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', newContract.tenant.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['property', updatedContract.property.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenants'],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.tenant.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['property', updatedContract.property.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.tenant.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['property', updatedContract.property.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', updatedContract.tenant.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
        queryKey: ['property', newContract.property.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenant', newContract.tenant.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
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
    mutationFn: (count: number) =>
      contractsApi.generateContractPayments(contractId, { count }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({
        queryKey: ['paymentsByContract', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      if (result.generated === result.requested) {
        showToast(
          `Generated ${result.generated} payment(s) successfully`,
          'success'
        );
      } else {
        showToast(
          `Generated ${result.generated} of ${result.requested} payment(s). Some dates already had payments.`,
          'success'
        );
      }
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
