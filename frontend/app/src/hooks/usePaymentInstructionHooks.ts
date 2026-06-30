import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getAll as getPaymentInstructions,
  getByIdentifier as getPaymentInstruction,
  createPaymentInstruction,
  updatePaymentInstruction,
  deletePaymentInstruction,
} from '../generated/api/payment-instructions/payment-instructions';
import {
  getHistory as getContractPaymentInstructions,
  getCurrent as getCurrentContractPaymentInstruction,
  createContractPaymentInstruction,
  updateContractPaymentInstruction,
  deleteContractPaymentInstruction,
} from '../generated/api/contract-payment-instructions/contract-payment-instructions';
import {
  CreatePaymentInstructionRequest,
  UpdatePaymentInstructionRequest,
  CreateContractPaymentInstructionRequest,
  UpdateContractPaymentInstructionRequest,
} from '../types/paymentInstruction';
import { queryKeys } from '../lib/queryKeys';

// Team-level template hooks

export const usePaymentInstructions = () => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.all(),
    queryFn: () => getPaymentInstructions(),
  });
};

export const usePaymentInstruction = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.detail(id),
    queryFn: () => getPaymentInstruction(id ?? ''),
    enabled: !!id,
  });
};

export const useCreatePaymentInstruction = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction created successfully',
    mutationFn: (data: CreatePaymentInstructionRequest) =>
      createPaymentInstruction(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
    },
  });
};

export const useUpdatePaymentInstruction = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction updated successfully',
    mutationFn: (data: UpdatePaymentInstructionRequest) =>
      updatePaymentInstruction(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.detail(id),
      });
    },
  });
};

export const useDeletePaymentInstruction = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction deleted successfully',
    mutationFn: (id: string) => deletePaymentInstruction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.paymentInstructions.all(),
      });
    },
  });
};

// Contract-level hooks

export const useContractPaymentInstructions = (
  contractId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.byContract(contractId),
    queryFn: () => getContractPaymentInstructions(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useCurrentContractPaymentInstruction = (
  contractId: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.paymentInstructions.currentByContract(contractId),
    queryFn: () => getCurrentContractPaymentInstruction(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useCreateContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction assigned successfully',
    mutationFn: (data: CreateContractPaymentInstructionRequest) =>
      createContractPaymentInstruction(contractId, data),
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
    },
  });
};

export const useUpdateContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction updated successfully',
    mutationFn: ({
      instructionId,
      data,
    }: {
      instructionId: string;
      data: UpdateContractPaymentInstructionRequest;
    }) => updateContractPaymentInstruction(contractId, instructionId, data),
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
    },
  });
};

export const useDeleteContractPaymentInstruction = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment instruction deleted successfully',
    mutationFn: (instructionId: string) =>
      deleteContractPaymentInstruction(contractId, instructionId),
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
    },
  });
};
