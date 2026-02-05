import client from './client';
import {
  PaymentResponse,
  PaymentReceivalResponse,
  CreatePaymentRequest,
  CreatePaymentReceivalRequest,
  UpdatePaymentReceivalRequest,
  UpdatePaymentRequest,
  MarkPaidRequest,
  BulkGeneratePaymentsRequest,
  GetPaymentsParams,
} from '../types/payment';
import { DocumentResponse, AuditLogEntry } from '../types/property';

export const getPayments = async (
  params?: GetPaymentsParams
): Promise<PaymentResponse[]> => {
  const response = await client.get('/payments', { params });
  return response.data;
};

export const getPayment = async (id: string): Promise<PaymentResponse> => {
  const response = await client.get(`/payments/${id}`);
  return response.data;
};

export const createPayment = async (
  data: CreatePaymentRequest
): Promise<PaymentResponse> => {
  const response = await client.post('/payments', data);
  return response.data;
};

export const updatePayment = async (
  id: string,
  data: UpdatePaymentRequest
): Promise<PaymentResponse> => {
  const response = await client.put(`/payments/${id}`, data);
  return response.data;
};

export const deletePayment = async (id: string): Promise<void> => {
  await client.delete(`/payments/${id}`);
};

export const markPaymentAsPaid = async (
  id: string,
  data: MarkPaidRequest
): Promise<PaymentResponse> => {
  const response = await client.put(`/payments/${id}/mark-paid`, data);
  return response.data;
};

export const bulkGeneratePayments = async (
  data: BulkGeneratePaymentsRequest
): Promise<PaymentResponse[]> => {
  const response = await client.post('/payments/bulk-generate', data);
  return response.data;
};

export const getOverduePayments = async (): Promise<PaymentResponse[]> => {
  const response = await client.get('/payments/overdue');
  return response.data;
};

export const getPaymentsByContract = async (
  contractId: string
): Promise<PaymentResponse[]> => {
  const response = await client.get('/payments', {
    params: { contractId },
  });
  return response.data;
};

export const uploadPaymentDocument = async (
  paymentId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

  const response = await client.post(
    `/payments/${paymentId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const deletePaymentDocument = async (
  paymentId: string,
  documentId: string
): Promise<void> => {
  await client.delete(`/payments/${paymentId}/documents/${documentId}`);
};

export const getPaymentDocuments = async (
  paymentId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/payments/${paymentId}/documents`);
  return response.data;
};

export const getPaymentAuditLog = async (
  paymentId: string
): Promise<AuditLogEntry[]> => {
  const response = await client.get(`/payments/${paymentId}/audit-log`);
  return response.data;
};

// Receival operations
export const registerReceival = async (
  paymentId: string,
  data: CreatePaymentReceivalRequest
): Promise<PaymentResponse> => {
  const response = await client.post(`/payments/${paymentId}/receivals`, data);
  return response.data;
};

export const getPaymentReceivals = async (
  paymentId: string
): Promise<PaymentReceivalResponse[]> => {
  const response = await client.get(`/payments/${paymentId}/receivals`);
  return response.data;
};

export const updatePaymentReceival = async (
  paymentId: string,
  receivalId: string,
  data: UpdatePaymentReceivalRequest
): Promise<PaymentResponse> => {
  const response = await client.put(
    `/payments/${paymentId}/receivals/${receivalId}`,
    data
  );
  return response.data;
};

export const deletePaymentReceival = async (
  paymentId: string,
  receivalId: string
): Promise<PaymentResponse> => {
  const response = await client.delete(
    `/payments/${paymentId}/receivals/${receivalId}`
  );
  return response.data;
};
