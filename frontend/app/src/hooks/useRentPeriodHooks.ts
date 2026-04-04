import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as rentPeriodsApi from '../api/rentPeriods';
import type { GenerateRentChangeDocumentsRequest } from '../api/rentPeriods';
import {
  CreateRentPeriodRequest,
  UpdateRentPeriodRequest,
} from '../types/contract';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useRentPeriods = (contractId: string | undefined) => {
  return useQuery({
    queryKey: ['rentPeriods', contractId],
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
      queryClient.invalidateQueries({ queryKey: ['rentPeriods', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['paymentsByContract', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
      queryClient.invalidateQueries({ queryKey: ['rentPeriods', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['paymentsByContract', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
      queryClient.invalidateQueries({ queryKey: ['rentPeriods', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['paymentsByContract', contractId],
      });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
        queryKey: ['contractDocuments', contractId],
      });
      queryClient.invalidateQueries({
        queryKey: ['contractAuditLog', contractId],
      });
      showToast('Documents generated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
