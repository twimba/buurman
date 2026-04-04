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
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useExpenses = (params?: GetExpensesParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.expenses.all(params),
    queryFn: () => expensesApi.getExpenses(params),
    placeholderData: keepPreviousData,
  });
};

export const useExpenseStats = () => {
  return useQuery({
    queryKey: queryKeys.expenses.stats(),
    queryFn: () => expensesApi.getExpenseStats(),
  });
};

export const useExpense = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.detail(id),
    queryFn: () => expensesApi.getExpense(id ?? ''),
    enabled: !!id,
  });
};

export const useExpensesByProperty = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.byProperty(propertyId),
    queryFn: () => expensesApi.getExpensesByProperty(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useExpenseSummary = (period: string) => {
  return useQuery({
    queryKey: queryKeys.expenses.summary(period),
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
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newExpense.property.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.financialOverview(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.incomeTrend(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.expenseBreakdown(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.propertyComparison(),
      });
      showToast('Expense created successfully', 'success');
      trackEvent(AnalyticsEvent.EXPENSE_CREATED);
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
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(
          updatedExpense.property.identifier
        ),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.financialOverview(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.incomeTrend(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.expenseBreakdown(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.propertyComparison(),
      });
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
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.financialOverview(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.incomeTrend(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.expenseBreakdown(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.reports.propertyComparison(),
      });
      showToast('Expense deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useExpenseDocuments = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.documents(expenseId),
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
        queryKey: queryKeys.expenses.documents(expenseId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.detail(expenseId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.auditLog(expenseId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
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
        queryKey: queryKeys.expenses.documents(expenseId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.detail(expenseId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.expenses.auditLog(expenseId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useExpenseAuditLog = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.auditLog(expenseId),
    queryFn: () => expensesApi.getExpenseAuditLog(expenseId ?? ''),
    enabled: !!expenseId,
  });
};
