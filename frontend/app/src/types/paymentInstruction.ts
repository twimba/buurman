// Response/request interfaces — re-exported from generated models
export type {
  PaymentInstructionResponse,
  CreatePaymentInstructionRequest,
  UpdatePaymentInstructionRequest,
  ContractPaymentInstructionResponse,
  CreateContractPaymentInstructionRequest,
  UpdateContractPaymentInstructionRequest,
} from '../generated/models';

// Enum — re-exported as value (orval enum is const + type of same name)
export {
  CreatePaymentInstructionRequestPaymentMethod as PaymentMethod,
  type CreatePaymentInstructionRequestPaymentMethod,
} from '../generated/models';

import { CreatePaymentInstructionRequestPaymentMethod } from '../generated/models';

// Frontend-only label map
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
