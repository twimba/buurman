import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getRentTimeline,
  addRentPeriod,
  updateRentPeriod,
  deleteRentPeriod,
} from '../generated/api/contract-rent-periods/contract-rent-periods';
import { generateRentChangeDocuments } from '../generated/api/contract-documents/contract-documents';
import type { GenerateRentChangeDocumentsRequest } from '../generated/models';
import {
  CreateRentPeriodRequest,
  UpdateRentPeriodRequest,
} from '../types/contract';
import { toRentComponentRequests } from '../utils/rentComponents';
import { queryKeys } from '../lib/queryKeys';

export const useRentPeriods = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.rentPeriods.all(contractId),
    queryFn: () => getRentTimeline(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useAddRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Rent period added successfully',
    mutationFn: (data: CreateRentPeriodRequest) =>
      addRentPeriod(contractId, {
        rentAmount: data.rentAmount,
        effectiveFrom: data.effectiveFrom,
        notes: data.notes,
        components: toRentComponentRequests(data.components),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rentPeriods.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useUpdateRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Rent period updated successfully',
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: UpdateRentPeriodRequest;
    }) =>
      updateRentPeriod(contractId, periodIdentifier, {
        rentAmount: data.rentAmount,
        effectiveFrom: data.effectiveFrom,
        notes: data.notes,
        components: toRentComponentRequests(data.components),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rentPeriods.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useDeleteRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Rent period deleted successfully',
    mutationFn: (periodIdentifier: string) =>
      deleteRentPeriod(contractId, periodIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rentPeriods.all(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.paymentsByContract(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
    },
  });
};

export const useGenerateRentChangeDocuments = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Documents generated successfully',
    mutationFn: ({
      periodId,
      request,
    }: {
      periodId: string;
      request: GenerateRentChangeDocumentsRequest;
    }) => generateRentChangeDocuments(contractId, periodId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
    },
  });
};
