import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getExpenses,
  getExpenseStats,
  getExpense,
  createExpense,
  updateExpense,
  deleteExpense,
  getExpenseDocuments,
  uploadExpenseDocument,
  deleteExpenseDocument,
  getExpenseAuditLog,
} from '../generated/api/expenses/expenses';
import type {
  GetExpensesParams,
  ExpenseResponseCategory,
} from '../generated/models';
import { CreateExpenseRequest, UpdateExpenseRequest } from '../types/expense';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
import { useAnnounce } from '@/hooks/useAnnounce';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useExpenses = (
  params?: {
    category?: ExpenseResponseCategory;
    propertyIdentifier?: string;
    contactIdentifier?: string;
    dateFrom?: string;
    dateTo?: string;
  } & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.expenses.all(params),
    queryFn: () => getExpenses(params as GetExpensesParams),
    placeholderData: keepPreviousData,
  });
};

export const useExpenseStats = () => {
  return useQuery({
    queryKey: queryKeys.expenses.stats(),
    queryFn: () => getExpenseStats(),
  });
};

export const useExpense = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.detail(id),
    queryFn: () => getExpense(id ?? ''),
    enabled: !!id,
  });
};

export const useExpensesByProperty = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.byProperty(propertyId),
    queryFn: () =>
      getExpenses({
        propertyIdentifier: propertyId ?? '',
        size: 1000,
      }).then((r) => r.content ?? []),
    enabled: !!propertyId,
  });
};

export const useCreateExpense = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreateExpenseRequest) => createExpense(data),
    onSuccess: (newExpense) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.expenses.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(newExpense.property?.identifier),
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
  });
};

export const useUpdateExpense = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Expense updated successfully',
    mutationFn: (data: UpdateExpenseRequest) => updateExpense(id, data),
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
          updatedExpense.property?.identifier
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
    },
  });
};

export const useDeleteExpense = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  return useMutation({
    mutationFn: (id: string) => deleteExpense(id),
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
      announce('Expense deleted');
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useExpenseDocuments = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.documents(expenseId),
    queryFn: () => getExpenseDocuments(expenseId ?? ''),
    enabled: !!expenseId,
  });
};

export const useUploadExpenseDocument = (expenseId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document uploaded successfully',
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadExpenseDocument(expenseId, { file }, { title, notes }),
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
    },
  });
};

export const useDeleteExpenseDocument = (expenseId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document deleted successfully',
    mutationFn: (documentId: string) => deleteExpenseDocument(documentId),
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
    },
  });
};

export const useExpenseAuditLog = (expenseId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.expenses.auditLog(expenseId),
    queryFn: () => getExpenseAuditLog(expenseId ?? ''),
    enabled: !!expenseId,
  });
};
