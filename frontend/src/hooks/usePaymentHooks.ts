import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const usePayments = (params?: GetPaymentsParams) => {
  return useQuery({
    queryKey: ['payments', params],
    queryFn: () => paymentsApi.getPayments(params),
  });
};

export const usePayment = (id: string | undefined) => {
  return useQuery({
    queryKey: ['payment', id],
    queryFn: () => paymentsApi.getPayment(id!),
    enabled: !!id,
  });
};

export const useOverduePayments = () => {
  return useQuery({
    queryKey: ['payments', 'overdue'],
    queryFn: () => paymentsApi.getOverduePayments(),
  });
};

export const usePaymentsByContract = (contractId: string | undefined) => {
  return useQuery({
    queryKey: ['payments', 'contract', contractId],
    queryFn: () => paymentsApi.getPaymentsByContract(contractId!),
    enabled: !!contractId,
  });
};

export const useCreatePayment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentsApi.createPayment(data),
    onSuccess: (newPayment) => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({
        queryKey: ['contracts'],
      });
      queryClient.invalidateQueries({
        queryKey: ['contract', newPayment.contract.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      showToast('Payment created successfully', 'success');
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['payment', id] });
      queryClient.invalidateQueries({
        queryKey: ['contract', updatedPayment.contract.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({
        queryKey: ['payment', updatedPayment.id],
      });
      queryClient.invalidateQueries({
        queryKey: ['contract', updatedPayment.contract.id],
      });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      showToast('Payment marked as paid successfully', 'success');
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['contracts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
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
    queryKey: ['paymentDocuments', paymentId],
    queryFn: () => paymentsApi.getPaymentDocuments(paymentId!),
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
        queryKey: ['paymentDocuments', paymentId],
      });
      queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
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
        queryKey: ['paymentDocuments', paymentId],
      });
      queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePaymentAuditLog = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: ['paymentAuditLog', paymentId],
    queryFn: () => paymentsApi.getPaymentAuditLog(paymentId!),
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['paymentAuditLog', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      showToast('Receival registered successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePaymentReceivals = (paymentId: string | undefined) => {
  return useQuery({
    queryKey: ['paymentReceivals', paymentId],
    queryFn: () => paymentsApi.getPaymentReceivals(paymentId!),
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['paymentAuditLog', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
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
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['payment', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['paymentAuditLog', paymentId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['financial-overview'] });
      queryClient.invalidateQueries({ queryKey: ['income-trend'] });
      showToast('Receival deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
