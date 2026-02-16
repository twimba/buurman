export enum PaymentMethod {
  BANK_TRANSFER = 'BANK_TRANSFER',
  PAYPAL = 'PAYPAL',
  CASH = 'CASH',
  CHECK = 'CHECK',
  DIRECT_DEBIT = 'DIRECT_DEBIT',
  IDEAL_WERO = 'IDEAL_WERO',
  ZELLE = 'ZELLE',
  OTHER = 'OTHER',
}

export const PaymentMethodLabels: Record<PaymentMethod, string> = {
  [PaymentMethod.BANK_TRANSFER]: 'Bank Transfer',
  [PaymentMethod.PAYPAL]: 'PayPal',
  [PaymentMethod.CASH]: 'Cash',
  [PaymentMethod.CHECK]: 'Check',
  [PaymentMethod.DIRECT_DEBIT]: 'Direct Debit',
  [PaymentMethod.IDEAL_WERO]: 'iDEAL / Wero',
  [PaymentMethod.ZELLE]: 'Zelle',
  [PaymentMethod.OTHER]: 'Other',
};

export interface PaymentInstructionResponse {
  identifier: string;
  name: string;
  description?: string;
  paymentMethod: PaymentMethod;
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

export interface CreatePaymentInstructionRequest {
  name: string;
  description?: string;
  paymentMethod: PaymentMethod;
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
  paymentMethod?: PaymentMethod;
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
