import client from './client';
import {
  PaymentResponse,
  CreatePaymentRequest,
  UpdatePaymentRequest,
  MarkPaidRequest,
  BulkGeneratePaymentsRequest,
  GetPaymentsParams,
} from '../types/payment';

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
