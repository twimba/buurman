import { ContractSummary } from './contract';
import { PropertySummary, DocumentResponse } from './property';
import { TenantSummary } from './tenant';

// Enum — re-exported from generated
export {
  PaymentResponseStatus as PaymentStatus,
  type PaymentResponseStatus,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import type { PaymentResponseStatus } from '../generated/models';

// Request interfaces — manual (generated adds to all optional fields)

export interface CreatePaymentRequest {
  contractIdentifier: string;
  amount: number;
  currency?: string;
  dueDate: string;
  notes?: string;
  markAsPaid?: boolean;
  paymentDate?: string;
}

export interface UpdatePaymentRequest {
  amount?: number;
  currency?: string;
  dueDate?: string;
  status?: PaymentResponseStatus;
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
  forMonth: string;
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
  status: PaymentResponseStatus;
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
  status: PaymentResponseStatus;
}

export interface GetPaymentsParams {
  status?: PaymentResponseStatus | 'OVERDUE';
  contractIdentifier?: string;
  propertyIdentifier?: string;
  dateFrom?: string;
  dateTo?: string;
}
