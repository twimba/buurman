import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as expensesApi from '../api/expenses';
import {
  CreateExpenseRequest,
  UpdateExpenseRequest,
  GetExpensesParams,
} from '../types/expense';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useExpenses = (params?: GetExpensesParams & PageParams) => {
  return useQuery({
    queryKey: ['expenses', params],
    queryFn: () => expensesApi.getExpenses(params),
    placeholderData: keepPreviousData,
  });
};

export const useExpenseStats = () => {
  return useQuery({
    queryKey: ['expenseStats'],
    queryFn: () => expensesApi.getExpenseStats(),
  });
};

export const useExpense = (id: string | undefined) => {
  return useQuery({
    queryKey: ['expense', id],
    queryFn: () => expensesApi.getExpense(id ?? ''),
    enabled: !!id,
  });
};

export const useExpensesByProperty = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['expenses', 'property', propertyId],
    queryFn: () => expensesApi.getExpensesByProperty(propertyId ?? ''),
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
      queryClient.invalidateQueries({ queryKey: ['expenseStats'] });
      queryClient.invalidateQueries({
        queryKey: ['properties'],
      });
      queryClient.invalidateQueries({
        queryKey: ['property', newExpense.property.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      queryClient.invalidateQueries({ queryKey: ['expense-breakdown'] });
      queryClient.invalidateQueries({ queryKey: ['property-comparison'] });
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
      queryClient.invalidateQueries({ queryKey: ['expenseStats'] });
      queryClient.invalidateQueries({ queryKey: ['expense', id] });
      queryClient.invalidateQueries({ queryKey: ['expenseAuditLog', id] });
      queryClient.invalidateQueries({
        queryKey: ['property', updatedExpense.property.identifier],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      queryClient.invalidateQueries({ queryKey: ['expense-breakdown'] });
      queryClient.invalidateQueries({ queryKey: ['property-comparison'] });
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
      queryClient.invalidateQueries({ queryKey: ['expenseStats'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      queryClient.invalidateQueries({ queryKey: ['expense-breakdown'] });
      queryClient.invalidateQueries({ queryKey: ['property-comparison'] });
      showToast('Expense deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useExpenseDocuments = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: ['expenseDocuments', expenseId],
    queryFn: () => expensesApi.getExpenseDocuments(expenseId ?? ''),
    enabled: !!expenseId,
  });
};

export const useUploadExpenseDocument = (expenseId: string) => {
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
    }) => expensesApi.uploadExpenseDocument(expenseId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['expenseDocuments', expenseId],
      });
      queryClient.invalidateQueries({ queryKey: ['expense', expenseId] });
      queryClient.invalidateQueries({
        queryKey: ['expenseAuditLog', expenseId],
      });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      showToast('Document uploaded successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteExpenseDocument = (expenseId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      expensesApi.deleteExpenseDocument(expenseId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['expenseDocuments', expenseId],
      });
      queryClient.invalidateQueries({ queryKey: ['expense', expenseId] });
      queryClient.invalidateQueries({
        queryKey: ['expenseAuditLog', expenseId],
      });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useExpenseAuditLog = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: ['expenseAuditLog', expenseId],
    queryFn: () => expensesApi.getExpenseAuditLog(expenseId ?? ''),
    enabled: !!expenseId,
  });
};
