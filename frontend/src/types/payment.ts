import { ContractSummary } from './contract';
import { PropertySummary, DocumentResponse } from './property';
import { TenantSummary } from './tenant';

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED',
}

export interface PaymentResponse {
  id: string;
  identifier: string;
  teamId: string;
  contract: ContractSummary;
  tenant: TenantSummary;
  property: PropertySummary;
  amount: number;
  currency: string;
  paymentDate?: string;
  dueDate: string;
  status: PaymentStatus;
  notes?: string;
  proofOfPayment?: DocumentResponse;
  receipt?: DocumentResponse;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentSummary {
  id: string;
  identifier: string;
  amount: number;
  currency: string;
  dueDate: string;
  paymentDate?: string;
  status: PaymentStatus;
}

export interface CreatePaymentRequest {
  contractId: string;
  amount: number;
  currency?: string;
  dueDate: string;
  notes?: string;
}

export interface UpdatePaymentRequest {
  amount?: number;
  currency?: string;
  dueDate?: string;
  status?: PaymentStatus;
  notes?: string;
}

export interface MarkPaidRequest {
  paymentDate: string;
  notes?: string;
}

export interface BulkGeneratePaymentsRequest {
  forMonth: string; // YYYY-MM format
}

export interface GetPaymentsParams {
  status?: PaymentStatus | 'OVERDUE';
  contractId?: string;
}
