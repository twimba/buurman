import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as paymentsApi from '../api/payments';
import {
  CreatePaymentRequest,
  CreatePaymentReceivalRequest,
  UpdatePaymentReceivalRequest,
  UpdatePaymentRequest,
  MarkPaidRequest,
  BulkGeneratePaymentsRequest,
  GetPaymentsParams,
} from '../types/payment';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const usePayments = (params?: GetPaymentsParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.payments.all(params),
    queryFn: () => paymentsApi.getPayments(params),
    placeholderData: keepPreviousData,
  });
};

export const usePaymentStats = () => {
  return useQuery({
    queryKey: queryKeys.payments.stats(),
    queryFn: () => paymentsApi.getPaymentStats(),
  });
};

export const usePayment = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.detail(id),
    queryFn: () => paymentsApi.getPayment(id ?? ''),
    enabled: !!id,
  });
};

export const useOverduePayments = () => {
  return useQuery({
    queryKey: queryKeys.payments.overdue(),
    queryFn: () => paymentsApi.getOverduePayments(),
  });
};

export const usePaymentsByContract = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.byContract(contractId),
    queryFn: () => paymentsApi.getPaymentsByContract(contractId ?? ''),
    enabled: !!contractId,
  });
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentsApi.createPayment(data),
    onSuccess: (newPayment) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.all(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(newPayment.contract.identifier),
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
      trackEvent(AnalyticsEvent.PAYMENT_CREATED);
      const verb = newPayment.status === 'PAID' ? 'registered' : 'scheduled';
      showToast(`Payment ${verb} successfully`, 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdatePayment = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdatePaymentRequest) =>
      paymentsApi.updatePayment(id, data),
    onSuccess: (updatedPayment) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(
          updatedPayment.contract.identifier
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
      showToast('Payment updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeletePayment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => paymentsApi.deletePayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
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
      showToast('Payment deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useMarkPaymentAsPaid = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: MarkPaidRequest }) =>
      paymentsApi.markPaymentAsPaid(id, data),
    onSuccess: (updatedPayment) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(updatedPayment.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(updatedPayment.identifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(
          updatedPayment.contract.identifier
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
      showToast('Payment marked as paid successfully', 'success');
      trackEvent(AnalyticsEvent.PAYMENT_MARKED_PAID);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useBulkGeneratePayments = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: BulkGeneratePaymentsRequest) =>
      paymentsApi.bulkGeneratePayments(data),
    onSuccess: (generatedPayments) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
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
      trackEvent(AnalyticsEvent.PAYMENTS_GENERATED, {
        count: generatedPayments.length,
      });
      showToast(
        `Generated ${generatedPayments.length} payment(s) successfully`,
        'success'
      );
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePaymentDocuments = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.documents(paymentId),
    queryFn: () => paymentsApi.getPaymentDocuments(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

export const useUploadPaymentDocument = (paymentId: string) => {
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
    }) => paymentsApi.uploadPaymentDocument(paymentId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.documents(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(paymentId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
      showToast('Document uploaded successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeletePaymentDocument = (paymentId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      paymentsApi.deletePaymentDocument(paymentId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.documents(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(paymentId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePaymentAuditLog = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.auditLog(paymentId),
    queryFn: () => paymentsApi.getPaymentAuditLog(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

// Receival hooks
export const useRegisterReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreatePaymentReceivalRequest) =>
      paymentsApi.registerReceival(paymentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(paymentId),
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
      showToast('Receival registered successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePaymentReceivals = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.receivals(paymentId),
    queryFn: () => paymentsApi.getPaymentReceivals(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

export const useUpdatePaymentReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      receivalId,
      data,
    }: {
      receivalId: string;
      data: UpdatePaymentReceivalRequest;
    }) => paymentsApi.updatePaymentReceival(paymentId, receivalId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(paymentId),
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
      showToast('Receival updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeletePaymentReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (receivalId: string) =>
      paymentsApi.deletePaymentReceival(paymentId, receivalId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.detail(paymentId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.payments.auditLog(paymentId),
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
      showToast('Receival deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
