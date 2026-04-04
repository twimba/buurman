import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as api from '../api/paymentInstructions';
import {
  CreatePaymentInstructionRequest,
  UpdatePaymentInstructionRequest,
  CreateContractPaymentInstructionRequest,
  UpdateContractPaymentInstructionRequest,
} from '../types/paymentInstruction';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

// Team-level template hooks

export const usePaymentInstructions = () => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.all(),
    queryFn: () => api.getPaymentInstructions(),
  });
};

export const usePaymentInstruction = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.detail(id),
    queryFn: () => api.getPaymentInstruction(id ?? ''),
    enabled: !!id,
  });
};

export const useCreatePaymentInstruction = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreatePaymentInstructionRequest) =>
      api.createPaymentInstruction(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
      showToast('Payment instruction created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdatePaymentInstruction = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdatePaymentInstructionRequest) =>
      api.updatePaymentInstruction(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.detail(id),
      });
      showToast('Payment instruction updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeletePaymentInstruction = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => api.deletePaymentInstruction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
      showToast('Payment instruction deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// Contract-level hooks

export const useContractPaymentInstructions = (
  contractId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.byContract(contractId),
    queryFn: () => api.getContractPaymentInstructions(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useCurrentContractPaymentInstruction = (
  contractId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.currentByContract(contractId),
    queryFn: () => api.getCurrentContractPaymentInstruction(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useCreateContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContractPaymentInstructionRequest) =>
      api.createContractPaymentInstruction(contractId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.byContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.currentByContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      showToast('Payment instruction assigned successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      instructionId,
      data,
    }: {
      instructionId: string;
      data: UpdateContractPaymentInstructionRequest;
    }) => api.updateContractPaymentInstruction(contractId, instructionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.byContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.currentByContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      showToast('Payment instruction updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (instructionId: string) =>
      api.deleteContractPaymentInstruction(contractId, instructionId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.byContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.currentByContract(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      showToast('Payment instruction deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
