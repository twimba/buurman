import { ContractSummary } from './contract';
import { PropertySummary, DocumentResponse } from './property';
import { TenantSummary } from './tenant';

export enum PaymentStatus {
  PENDING = 'PENDING',
  PARTIALLY_PAID = 'PARTIALLY_PAID',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED',
}

export interface PaymentReceivalResponse {
  identifier: string;
  amount: number;
  receivalDate: string;
  notes?: string;
  createdAt: string;
}

export interface PaymentResponse {
  identifier: string;
  contract: ContractSummary;
  tenant: TenantSummary;
  property: PropertySummary;
  amount: number;
  currency: string;
  receivedAmount: number;
  balance: number;
  paymentDate?: string;
  dueDate: string;
  status: PaymentStatus;
  notes?: string;
  proofOfPayment?: DocumentResponse;
  receipt?: DocumentResponse;
  receivals: PaymentReceivalResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface PaymentSummary {
  identifier: string;
  amount: number;
  currency: string;
  dueDate: string;
  paymentDate?: string;
  status: PaymentStatus;
}

export interface CreatePaymentRequest {
  contractIdentifier: string;
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

export interface CreatePaymentReceivalRequest {
  amount: number;
  receivalDate: string;
  notes?: string;
}

export interface UpdatePaymentReceivalRequest {
  amount: number;
  receivalDate: string;
  notes?: string;
}

export interface BulkGeneratePaymentsRequest {
  forMonth: string; // YYYY-MM format
}

export interface GetPaymentsParams {
  status?: PaymentStatus | 'OVERDUE';
  contractIdentifier?: string;
}
