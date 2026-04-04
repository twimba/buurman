import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as rentPeriodsApi from '../api/rentPeriods';
import type { GenerateRentChangeDocumentsRequest } from '../api/rentPeriods';
import {
  CreateRentPeriodRequest,
  UpdateRentPeriodRequest,
} from '../types/contract';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useRentPeriods = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.rentPeriods.all(contractId),
    queryFn: () => rentPeriodsApi.getRentPeriods(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useAddRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateRentPeriodRequest) =>
      rentPeriodsApi.addRentPeriod(contractId, data),
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
      showToast('Rent period added successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      periodIdentifier,
      data,
    }: {
      periodIdentifier: string;
      data: UpdateRentPeriodRequest;
    }) => rentPeriodsApi.updateRentPeriod(contractId, periodIdentifier, data),
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
      showToast('Rent period updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteRentPeriod = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (periodIdentifier: string) =>
      rentPeriodsApi.deleteRentPeriod(contractId, periodIdentifier),
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
      showToast('Rent period deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useGenerateRentChangeDocuments = (contractId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      periodId,
      request,
    }: {
      periodId: string;
      request: GenerateRentChangeDocumentsRequest;
    }) =>
      rentPeriodsApi.generateRentChangeDocuments(contractId, periodId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.documents(contractId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.auditLog(contractId),
      });
      showToast('Documents generated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
