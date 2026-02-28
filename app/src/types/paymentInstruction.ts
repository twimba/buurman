// Enum — re-exported from generated
export {
  CreatePaymentInstructionRequestPaymentMethod as PaymentMethod,
  type CreatePaymentInstructionRequestPaymentMethod,
} from '../generated/models';

// Interfaces — kept manual (generated uses `string` for enum fields)

import { CreatePaymentInstructionRequestPaymentMethod } from '../generated/models';

// Request interfaces — manual (generated adds | null to all optional fields)

export interface CreatePaymentInstructionRequest {
  name: string;
  description?: string;
  paymentMethod: CreatePaymentInstructionRequestPaymentMethod;
  bankName?: string;
  accountHolderName?: string;
  iban?: string;
  bicSwift?: string;
  accountNumber?: string;
  routingNumber?: string;
  paymentReference?: string;
  additionalDetails?: string;
  isDefault?: boolean;
}

export interface UpdatePaymentInstructionRequest {
  name?: string;
  description?: string;
  paymentMethod?: CreatePaymentInstructionRequestPaymentMethod;
  bankName?: string;
  accountHolderName?: string;
  iban?: string;
  bicSwift?: string;
  accountNumber?: string;
  routingNumber?: string;
  paymentReference?: string;
  additionalDetails?: string;
  isDefault?: boolean;
}

export interface CreateContractPaymentInstructionRequest {
  paymentInstructionIdentifier?: string;
  isCustom?: boolean;
  customName?: string;
  customDescription?: string;
  customPaymentMethod?: string;
  customBankName?: string;
  customAccountHolderName?: string;
  customIban?: string;
  customBicSwift?: string;
  customAccountNumber?: string;
  customRoutingNumber?: string;
  customPaymentReference?: string;
  customAdditionalDetails?: string;
  effectiveFrom: string;
  notes?: string;
}

export type UpdateContractPaymentInstructionRequest =
  CreateContractPaymentInstructionRequest;

export interface PaymentInstructionResponse {
  identifier: string;
  name: string;
  description?: string;
  paymentMethod: CreatePaymentInstructionRequestPaymentMethod;
  bankName?: string;
  accountHolderName?: string;
  iban?: string;
  bicSwift?: string;
  accountNumber?: string;
  routingNumber?: string;
  paymentReference?: string;
  additionalDetails?: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ContractPaymentInstructionResponse {
  identifier: string;
  paymentInstructionIdentifier?: string;
  isCustom: boolean;
  name: string;
  description?: string;
  paymentMethod: string;
  bankName?: string;
  accountHolderName?: string;
  iban?: string;
  bicSwift?: string;
  accountNumber?: string;
  routingNumber?: string;
  paymentReference?: string;
  additionalDetails?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export const PaymentMethodLabels: Record<
  CreatePaymentInstructionRequestPaymentMethod,
  string
> = {
  [CreatePaymentInstructionRequestPaymentMethod.BANK_TRANSFER]: 'Bank Transfer',
  [CreatePaymentInstructionRequestPaymentMethod.PAYPAL]: 'PayPal',
  [CreatePaymentInstructionRequestPaymentMethod.CASH]: 'Cash',
  [CreatePaymentInstructionRequestPaymentMethod.CHECK]: 'Check',
  [CreatePaymentInstructionRequestPaymentMethod.DIRECT_DEBIT]: 'Direct Debit',
  [CreatePaymentInstructionRequestPaymentMethod.IDEAL_WERO]: 'iDEAL / Wero',
  [CreatePaymentInstructionRequestPaymentMethod.ZELLE]: 'Zelle',
  [CreatePaymentInstructionRequestPaymentMethod.OTHER]: 'Other',
};
