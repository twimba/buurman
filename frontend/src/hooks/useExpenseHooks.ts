import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as expensesApi from '../api/expenses';
import {
  CreateExpenseRequest,
  UpdateExpenseRequest,
  GetExpensesParams,
} from '../types/expense';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useExpenses = (params?: GetExpensesParams) => {
  return useQuery({
    queryKey: ['expenses', params],
    queryFn: () => expensesApi.getExpenses(params),
  });
};

export const useExpense = (id: string | undefined) => {
  return useQuery({
    queryKey: ['expense', id],
    queryFn: () => expensesApi.getExpense(id!),
    enabled: !!id,
  });
};

export const useExpensesByProperty = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['expenses', 'property', propertyId],
    queryFn: () => expensesApi.getExpensesByProperty(propertyId!),
    enabled: !!propertyId,
  });
};

export const useExpenseSummary = (period: string) => {
  return useQuery({
    queryKey: ['expenses', 'summary', period],
    queryFn: () => expensesApi.getExpenseSummary(period),
    enabled: !!period,
  });
};

export const useCreateExpense = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateExpenseRequest) => expensesApi.createExpense(data),
    onSuccess: (newExpense) => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', newExpense.property.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      showToast('Expense created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateExpense = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateExpenseRequest) =>
      expensesApi.updateExpense(id, data),
    onSuccess: (updatedExpense) => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['expense', id] });
      queryClient.invalidateQueries({
        queryKey: ['property', updatedExpense.property.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      showToast('Expense updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteExpense = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => expensesApi.deleteExpense(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      showToast('Expense deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
