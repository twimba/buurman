import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getPayments,
  getPaymentStats,
  getPayment,
  getOverduePayments,
  createPayment,
  updatePayment,
  deletePayment,
  markPaymentAsPaid,
  bulkGeneratePayments,
  getPaymentDocuments,
  uploadPaymentDocument,
  deletePaymentDocument,
  getPaymentAuditLog,
  registerReceival,
  getReceivals,
  updateReceival,
  deleteReceival,
  getPaymentArrears,
  getPaymentReminders,
  sendPaymentReminder,
  bulkMarkPaymentsAsPaid,
  bulkSendPaymentReminders,
  cancelPayment,
  bulkCancelPayments,
  waiveLateFee,
  writeOffPayment,
  applyCreditToPayment,
} from '../generated/api/payments/payments';
import type {
  GetPaymentsParams,
  PaymentResponseStatus,
} from '../generated/models';
import {
  CreatePaymentRequest,
  CreatePaymentReceivalRequest,
  UpdatePaymentReceivalRequest,
  UpdatePaymentRequest,
  MarkPaidRequest,
  BulkGeneratePaymentsRequest,
  SendPaymentReminderRequest,
  BulkMarkPaidRequest,
  BulkSendPaymentRemindersRequest,
  CancelPaymentRequest,
  BulkCancelPaymentsRequest,
  WaiveLateFeeRequest,
  WriteOffPaymentRequest,
  ApplyCreditRequest,
} from '../types/payment';
import { useTranslation } from 'react-i18next';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
import { useAnnounce } from '@/hooks/useAnnounce';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

/**
 * Invalidate what a payment change touches. Lists and stats always; arrears, dashboards and
 * reports only when money or status moved (`financial`), since those are the expensive queries.
 */
const invalidatePaymentViews = (
  queryClient: ReturnType<typeof useQueryClient>,
  { financial = true, contractId }: { financial?: boolean; contractId?: string } = {}
) => {
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.all() });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.stats() });
  if (contractId) {
    queryClient.invalidateQueries({ queryKey: queryKeys.contracts.detail(contractId) });
  }
  if (!financial) {
    return;
  }
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.arrears() });
  queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.stats() });
  queryClient.invalidateQueries({
    queryKey: queryKeys.dashboard.propertyDashboard(),
  });
  queryClient.invalidateQueries({
    queryKey: queryKeys.reports.financialOverview(),
  });
  queryClient.invalidateQueries({ queryKey: queryKeys.reports.incomeTrend() });
};

/** Detail, audit log and receivals of one payment. */
const invalidatePaymentDetail = (
  queryClient: ReturnType<typeof useQueryClient>,
  id: string
) => {
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.detail(id) });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.auditLog(id) });
  queryClient.invalidateQueries({ queryKey: queryKeys.payments.receivals(id) });
};

export const usePayments = (
  params?: {
    status?: PaymentResponseStatus | 'OVERDUE';
    contractIdentifier?: string;
    propertyIdentifier?: string;
    contactIdentifier?: string;
    dateFrom?: string;
    dateTo?: string;
  } & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.payments.all(params),
    queryFn: () => getPayments(params as GetPaymentsParams),
    placeholderData: keepPreviousData,
  });
};

export const usePaymentStats = () => {
  return useQuery({
    queryKey: queryKeys.payments.stats(),
    queryFn: () => getPaymentStats(),
  });
};

export const usePayment = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.detail(id),
    queryFn: () => getPayment(id ?? ''),
    enabled: !!id,
  });
};

export const useOverduePayments = () => {
  return useQuery({
    queryKey: queryKeys.payments.overdue(),
    queryFn: () => getOverduePayments(),
  });
};

export const usePaymentsByContract = (contractId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.byContract(contractId),
    queryFn: () =>
      getPayments({ contractIdentifier: contractId ?? '', size: 1000 }).then(
        (r) => r.content ?? []
      ),
    enabled: !!contractId,
  });
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreatePaymentRequest) => createPayment(data),
    onSuccess: (newPayment) => {
      invalidatePaymentViews(queryClient, {
        contractId: newPayment.contract?.identifier,
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      trackEvent(AnalyticsEvent.PAYMENT_CREATED);
      const verb = newPayment.status === 'PAID' ? 'registered' : 'scheduled';
      showToast(`Payment ${verb} successfully`, 'success');
    },
  });
};

export const useUpdatePayment = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Payment updated successfully',
    mutationFn: (data: UpdatePaymentRequest) => updatePayment(id, data),
    onSuccess: (updatedPayment) => {
      invalidatePaymentViews(queryClient, {
        contractId: updatedPayment.contract?.identifier,
      });
      invalidatePaymentDetail(queryClient, id);
    },
  });
};

export const useDeletePayment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  return useMutation({
    mutationFn: (id: string) => deletePayment(id),
    onSuccess: () => {
      invalidatePaymentViews(queryClient);
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      showToast('Payment deleted successfully', 'success');
      announce('Payment deleted');
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useMarkPaymentAsPaid = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: MarkPaidRequest }) =>
      markPaymentAsPaid(id, data),
    onSuccess: (updatedPayment) => {
      invalidatePaymentViews(queryClient, {
        contractId: updatedPayment.contract?.identifier,
      });
      invalidatePaymentDetail(queryClient, updatedPayment.identifier);
      showToast('Payment marked as paid successfully', 'success');
      announce('Payment marked as paid');
      trackEvent(AnalyticsEvent.PAYMENT_MARKED_PAID);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useBulkGeneratePayments = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: BulkGeneratePaymentsRequest) =>
      bulkGeneratePayments(data),
    onSuccess: (generatedPayments) => {
      invalidatePaymentViews(queryClient);
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
      trackEvent(AnalyticsEvent.PAYMENTS_GENERATED, {
        count: generatedPayments.length,
      });
      showToast(
        `Generated ${generatedPayments.length} payment(s) successfully`,
        'success'
      );
    },
  });
};

export const usePaymentDocuments = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.documents(paymentId),
    queryFn: () => getPaymentDocuments(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

export const useUploadPaymentDocument = (paymentId: string) => {
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
    }) => uploadPaymentDocument(paymentId, { file }, { title, notes }),
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
    },
  });
};

export const useDeletePaymentDocument = (paymentId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document deleted successfully',
    mutationFn: (documentId: string) => deletePaymentDocument(documentId),
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
    },
  });
};

export const usePaymentAuditLog = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.auditLog(paymentId),
    queryFn: () => getPaymentAuditLog(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

// Receival hooks
export const useRegisterReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Receival registered successfully',
    mutationFn: (data: CreatePaymentReceivalRequest) =>
      registerReceival(paymentId, data),
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
    },
  });
};

export const usePaymentReceivals = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.receivals(paymentId),
    queryFn: () => getReceivals(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

export const useUpdatePaymentReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Receival updated successfully',
    mutationFn: ({
      receivalId,
      data,
    }: {
      receivalId: string;
      data: UpdatePaymentReceivalRequest;
    }) => updateReceival(paymentId, receivalId, data),
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
    },
  });
};

export const useDeletePaymentReceival = (paymentId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Receival deleted successfully',
    mutationFn: (receivalId: string) => deleteReceival(paymentId, receivalId),
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
    },
  });
};

// --- Arrears & reminders (BUUR-101) ---

export const usePaymentArrears = () => {
  return useQuery({
    queryKey: queryKeys.payments.arrears(),
    queryFn: () => getPaymentArrears(),
  });
};

export const usePaymentReminders = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.payments.reminders(paymentId),
    queryFn: () => getPaymentReminders(paymentId ?? ''),
    enabled: !!paymentId,
  });
};

export const useSendPaymentReminder = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  const { t } = useTranslation('payments');
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: SendPaymentReminderRequest }) =>
      sendPaymentReminder(id, data),
    onSuccess: (reminder, { id }) => {
      // Not a financial change: the timeline and the arrears "last reminded" column only.
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.reminders(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.arrears() });
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.auditLog(id) });
      const message = t('toasts.reminderSent', {
        email: reminder.recipientEmail ?? '',
      });
      showToast(message, 'success');
      announce(message);
      trackEvent(AnalyticsEvent.PAYMENT_REMINDER_SENT);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useBulkMarkPaymentsAsPaid = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  const { t } = useTranslation('payments');
  return useMutation({
    mutationFn: (data: BulkMarkPaidRequest) => bulkMarkPaymentsAsPaid(data),
    onSuccess: (results) => {
      invalidatePaymentViews(queryClient);
      results.forEach((r) =>
        queryClient.invalidateQueries({
          queryKey: queryKeys.payments.detail(r.identifier),
        })
      );
      const success = results.filter((r) => !r.error).length;
      const failed = results.length - success;
      const message = t('toasts.bulkMarkPaid', { success, failed });
      showToast(message, failed > 0 && success === 0 ? 'error' : 'success');
      announce(message);
      trackEvent(AnalyticsEvent.PAYMENTS_BULK_MARKED_PAID);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useBulkSendPaymentReminders = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  const { t } = useTranslation('payments');
  return useMutation({
    mutationFn: (data: BulkSendPaymentRemindersRequest) =>
      bulkSendPaymentReminders(data),
    onSuccess: (results) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.payments.arrears() });
      results.forEach((r) =>
        queryClient.invalidateQueries({
          queryKey: queryKeys.payments.reminders(r.identifier),
        })
      );
      const success = results.filter((r) => !r.error).length;
      const failed = results.length - success;
      const message = t('toasts.bulkReminders', { success, failed });
      showToast(message, failed > 0 && success === 0 ? 'error' : 'success');
      announce(message);
      trackEvent(AnalyticsEvent.PAYMENTS_BULK_REMINDERS_SENT);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

// --- Cancellations, late fees, write-offs, credits (BUUR-101) ---

const usePaymentActionMutation = <TVars extends { id: string }>(
  fn: (vars: TVars) => Promise<import('../types/payment').PaymentResponse>,
  successKey: string,
  event: (typeof AnalyticsEvent)[keyof typeof AnalyticsEvent]
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  const { t } = useTranslation('payments');
  return useMutation({
    mutationFn: fn,
    onSuccess: (updated, { id }) => {
      invalidatePaymentViews(queryClient, {
        contractId: updated.contract?.identifier,
      });
      invalidatePaymentDetail(queryClient, id);
      if (updated.contact?.identifier) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.contacts.credits(updated.contact.identifier),
        });
      }
      const message = t(successKey);
      showToast(message, 'success');
      announce(message);
      trackEvent(event);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};

export const useCancelPayment = () =>
  usePaymentActionMutation(
    ({ id, data }: { id: string; data: CancelPaymentRequest }) =>
      cancelPayment(id, data),
    'toasts.cancelled',
    AnalyticsEvent.PAYMENT_CANCELLED
  );

export const useWaiveLateFee = () =>
  usePaymentActionMutation(
    ({ id, data }: { id: string; data: WaiveLateFeeRequest }) =>
      waiveLateFee(id, data),
    'toasts.lateFeeWaived',
    AnalyticsEvent.LATE_FEE_WAIVED
  );

export const useWriteOffPayment = () =>
  usePaymentActionMutation(
    ({ id, data }: { id: string; data: WriteOffPaymentRequest }) =>
      writeOffPayment(id, data),
    'toasts.writtenOff',
    AnalyticsEvent.PAYMENT_WRITTEN_OFF
  );

export const useApplyCredit = () =>
  usePaymentActionMutation(
    ({ id, data }: { id: string; data: ApplyCreditRequest }) =>
      applyCreditToPayment(id, data),
    'toasts.creditApplied',
    AnalyticsEvent.CREDIT_APPLIED
  );

export const useBulkCancelPayments = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const announce = useAnnounce();
  const { t } = useTranslation('payments');
  return useMutation({
    mutationFn: (data: BulkCancelPaymentsRequest) => bulkCancelPayments(data),
    onSuccess: (results) => {
      invalidatePaymentViews(queryClient);
      results.forEach((r) =>
        queryClient.invalidateQueries({
          queryKey: queryKeys.payments.detail(r.identifier),
        })
      );
      const success = results.filter((r) => !r.error).length;
      const failed = results.length - success;
      const message = t('toasts.bulkCancelled', { success, failed });
      showToast(message, failed > 0 && success === 0 ? 'error' : 'success');
      announce(message);
      trackEvent(AnalyticsEvent.PAYMENTS_BULK_CANCELLED);
    },
    onError: (error) => {
      const message = getErrorMessage(error);
      showToast(message, 'error');
      announce(message, { assertive: true });
    },
  });
};
