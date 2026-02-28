// ============================================================
// Enums — re-exported from generated
// ============================================================

export {
  PropertyAcquisitionResponseAcquisitionType as AcquisitionType,
  type PropertyAcquisitionResponseAcquisitionType,
} from '../generated/models';

// DepreciationMethod — generated type includes | null; re-export value and non-nullable type
import { PropertyAcquisitionResponseDepreciationMethod as _DepreciationMethodConst } from '../generated/models';
export const DepreciationMethod = _DepreciationMethodConst;
export type DepreciationMethod =
  (typeof _DepreciationMethodConst)[keyof typeof _DepreciationMethodConst];
export type PropertyAcquisitionResponseDepreciationMethod = DepreciationMethod;

export {
  PropertyValuationResponseValuationType as ValuationType,
  type PropertyValuationResponseValuationType,
} from '../generated/models';

export {
  PropertyFinancingResponseFinancingType as FinancingType,
  type PropertyFinancingResponseFinancingType,
} from '../generated/models';

export {
  PropertyFinancingResponseRateType as RateType,
  type PropertyFinancingResponseRateType,
} from '../generated/models';

export {
  PropertyFinancingResponseStatus as FinancingStatus,
  type PropertyFinancingResponseStatus,
} from '../generated/models';

export {
  FinancingPaymentResponseStatus as PaymentStatus,
  type FinancingPaymentResponseStatus,
} from '../generated/models';

export {
  PropertyInsuranceResponseInsuranceType as InsuranceType,
  type PropertyInsuranceResponseInsuranceType,
} from '../generated/models';

export {
  PropertyInsuranceResponseStatus as InsuranceStatus,
  type PropertyInsuranceResponseStatus,
} from '../generated/models';

export {
  PropertyTaxResponseTaxType as TaxType,
  type PropertyTaxResponseTaxType,
} from '../generated/models';

export {
  PropertyTaxResponseStatus as TaxStatus,
  type PropertyTaxResponseStatus,
} from '../generated/models';

export {
  PropertyFeeResponseFeeType as FeeType,
  type PropertyFeeResponseFeeType,
} from '../generated/models';

export {
  PropertyFeeResponseStatus as FeeStatus,
  type PropertyFeeResponseStatus,
} from '../generated/models';

// PaymentFrequency — generated uses `string` for this field,
// keep manual enum for full type safety (includes SEMI_ANNUALLY, CUSTOM)
export enum PaymentFrequency {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  SEMI_ANNUALLY = 'SEMI_ANNUALLY',
  ANNUALLY = 'ANNUALLY',
  CUSTOM = 'CUSTOM',
}

// ============================================================
// Imports for manual interfaces and constants
// ============================================================

import { PropertyAcquisitionResponseAcquisitionType } from '../generated/models';
import { PropertyValuationResponseValuationType } from '../generated/models';
import { PropertyFinancingResponseFinancingType } from '../generated/models';
import { PropertyFinancingResponseRateType } from '../generated/models';
import { PropertyFinancingResponseStatus } from '../generated/models';
import { FinancingPaymentResponseStatus } from '../generated/models';
import { PropertyInsuranceResponseInsuranceType } from '../generated/models';
import { PropertyInsuranceResponseStatus } from '../generated/models';
import { PropertyTaxResponseTaxType } from '../generated/models';
import { PropertyTaxResponseStatus } from '../generated/models';
import { PropertyFeeResponseFeeType } from '../generated/models';
import { PropertyFeeResponseStatus } from '../generated/models';

// ============================================================
// Response interfaces — kept manual (generated uses `string` for enum fields)
// ============================================================

export interface PropertyAcquisitionResponse {
  identifier: string;
  acquisitionType: PropertyAcquisitionResponseAcquisitionType;
  acquisitionDate?: string;
  purchasePrice?: number;
  purchasePriceCurrency?: string;
  closingCosts?: number;
  closingCostsCurrency?: string;
  renovationCosts?: number;
  renovationCostsCurrency?: string;
  landValue?: number;
  landValueCurrency?: string;
  depreciationMethod?: PropertyAcquisitionResponseDepreciationMethod;
  depreciationYears?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyValuationResponse {
  identifier: string;
  valuationType: PropertyValuationResponseValuationType;
  valuationDate: string;
  amount: number;
  currency: string;
  source?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyFinancingResponse {
  identifier: string;
  propertyIdentifier: string;
  financingType: PropertyFinancingResponseFinancingType;
  rateType: PropertyFinancingResponseRateType;
  lenderName?: string;
  loanNumber?: string;
  originalAmount: number;
  originalAmountCurrency: string;
  currentBalance?: number;
  currentBalanceCurrency?: string;
  interestRate?: number;
  monthlyPayment?: number;
  monthlyPaymentCurrency?: string;
  paymentVariable: boolean;
  startDate: string;
  endDate?: string;
  termMonths?: number;
  status: PropertyFinancingResponseStatus;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface FinancingPaymentResponse {
  identifier: string;
  financingIdentifier: string;
  paymentDate: string;
  totalAmount: number;
  principalAmount?: number;
  interestAmount?: number;
  escrowAmount?: number;
  extraPayment?: number;
  currency: string;
  status: FinancingPaymentResponseStatus;
  notes?: string;
  balanceDeducted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyInsuranceResponse {
  identifier: string;
  propertyIdentifier: string;
  insuranceType: PropertyInsuranceResponseInsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium: number;
  annualPremiumCurrency: string;
  paymentFrequency: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status: PropertyInsuranceResponseStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyTaxResponse {
  identifier: string;
  propertyIdentifier: string;
  taxType: PropertyTaxResponseTaxType;
  authority?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status: PropertyTaxResponseStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyFeeResponse {
  identifier: string;
  propertyIdentifier: string;
  feeType: PropertyFeeResponseFeeType;
  name?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status: PropertyFeeResponseStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyFinancialSummaryResponse {
  acquisition?: PropertyAcquisitionResponse;
  latestValuation?: PropertyValuationResponse;
  valuationHistory: PropertyValuationResponse[];
  financings: PropertyFinancingResponse[];
  insurances: PropertyInsuranceResponse[];
  taxes: PropertyTaxResponse[];
  fees: PropertyFeeResponse[];
  totalFinancingBalance?: number;
  totalAnnualInsurance?: number;
  totalAnnualTaxes?: number;
  totalAnnualFees?: number;
  totalAnnualCosts?: number;
  netWorth?: number;
  currency?: string;
}

// ============================================================
// Request interfaces — manual (generated adds | null to all optional fields)
// ============================================================

export interface UpsertPropertyAcquisitionRequest {
  acquisitionType: PropertyAcquisitionResponseAcquisitionType;
  acquisitionDate?: string;
  purchasePrice?: number;
  purchasePriceCurrency?: string;
  closingCosts?: number;
  closingCostsCurrency?: string;
  renovationCosts?: number;
  renovationCostsCurrency?: string;
  landValue?: number;
  landValueCurrency?: string;
  depreciationMethod?: PropertyAcquisitionResponseDepreciationMethod;
  depreciationYears?: number;
  notes?: string;
}

export interface CreatePropertyValuationRequest {
  valuationType: PropertyValuationResponseValuationType;
  valuationDate: string;
  amount: number;
  currency: string;
  source?: string;
  notes?: string;
}

export interface UpdatePropertyValuationRequest {
  valuationType?: PropertyValuationResponseValuationType;
  valuationDate?: string;
  amount?: number;
  currency?: string;
  source?: string;
  notes?: string;
}

export interface CreatePropertyFinancingRequest {
  financingType: PropertyFinancingResponseFinancingType;
  rateType?: PropertyFinancingResponseRateType;
  lenderName?: string;
  loanNumber?: string;
  originalAmount: number;
  originalAmountCurrency: string;
  currentBalance?: number;
  currentBalanceCurrency?: string;
  interestRate?: number;
  monthlyPayment?: number;
  monthlyPaymentCurrency?: string;
  paymentVariable?: boolean;
  startDate: string;
  endDate?: string;
  termMonths?: number;
  status?: PropertyFinancingResponseStatus;
  notes?: string;
}

export interface UpdatePropertyFinancingRequest {
  financingType?: PropertyFinancingResponseFinancingType;
  rateType?: PropertyFinancingResponseRateType;
  lenderName?: string;
  loanNumber?: string;
  originalAmount?: number;
  originalAmountCurrency?: string;
  currentBalance?: number;
  currentBalanceCurrency?: string;
  interestRate?: number;
  monthlyPayment?: number;
  monthlyPaymentCurrency?: string;
  paymentVariable?: boolean;
  startDate?: string;
  endDate?: string;
  termMonths?: number;
  status?: PropertyFinancingResponseStatus;
  notes?: string;
}

export interface CreateFinancingPaymentRequest {
  paymentDate: string;
  totalAmount: number;
  principalAmount?: number;
  interestAmount?: number;
  escrowAmount?: number;
  extraPayment?: number;
  currency: string;
  status?: FinancingPaymentResponseStatus;
  notes?: string;
  deductFromBalance?: boolean;
}

export interface UpdateFinancingPaymentRequest {
  paymentDate?: string;
  totalAmount?: number;
  principalAmount?: number;
  interestAmount?: number;
  escrowAmount?: number;
  extraPayment?: number;
  currency?: string;
  status?: FinancingPaymentResponseStatus;
  notes?: string;
  deductFromBalance?: boolean;
}

export interface CreatePropertyInsuranceRequest {
  insuranceType: PropertyInsuranceResponseInsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium: number;
  annualPremiumCurrency: string;
  paymentFrequency?: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status?: PropertyInsuranceResponseStatus;
  notes?: string;
}

export interface UpdatePropertyInsuranceRequest {
  insuranceType?: PropertyInsuranceResponseInsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium?: number;
  annualPremiumCurrency?: string;
  paymentFrequency?: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status?: PropertyInsuranceResponseStatus;
  notes?: string;
}

export interface CreatePropertyTaxRequest {
  taxType: PropertyTaxResponseTaxType;
  authority?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status?: PropertyTaxResponseStatus;
  notes?: string;
}

export interface UpdatePropertyTaxRequest {
  taxType?: PropertyTaxResponseTaxType;
  authority?: string;
  annualAmount?: number;
  currency?: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status?: PropertyTaxResponseStatus;
  notes?: string;
}

export interface CreatePropertyFeeRequest {
  feeType: PropertyFeeResponseFeeType;
  name?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status?: PropertyFeeResponseStatus;
  notes?: string;
}

export interface UpdatePropertyFeeRequest {
  feeType?: PropertyFeeResponseFeeType;
  name?: string;
  annualAmount?: number;
  currency?: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status?: PropertyFeeResponseStatus;
  notes?: string;
}

// ============================================================
// Label formatters
// ============================================================

export const formatAcquisitionType = (
  type: PropertyAcquisitionResponseAcquisitionType
): string => {
  const labels: Record<PropertyAcquisitionResponseAcquisitionType, string> = {
    [PropertyAcquisitionResponseAcquisitionType.PURCHASE]: 'Purchase',
    [PropertyAcquisitionResponseAcquisitionType.INHERITANCE]: 'Inheritance',
    [PropertyAcquisitionResponseAcquisitionType.GIFT]: 'Gift',
    [PropertyAcquisitionResponseAcquisitionType.FORECLOSURE]: 'Foreclosure',
    [PropertyAcquisitionResponseAcquisitionType.AUCTION]: 'Auction',
    [PropertyAcquisitionResponseAcquisitionType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatDepreciationMethod = (
  method: PropertyAcquisitionResponseDepreciationMethod
): string => {
  const labels: Record<PropertyAcquisitionResponseDepreciationMethod, string> =
    {
      [_DepreciationMethodConst.STRAIGHT_LINE]: 'Straight Line',
      [_DepreciationMethodConst.DECLINING_BALANCE]: 'Declining Balance',
      [_DepreciationMethodConst.NONE]: 'None',
    };
  return labels[method];
};

export const formatValuationType = (
  type: PropertyValuationResponseValuationType
): string => {
  const labels: Record<PropertyValuationResponseValuationType, string> = {
    [PropertyValuationResponseValuationType.MARKET]: 'Market',
    [PropertyValuationResponseValuationType.APPRAISAL]: 'Appraisal',
    [PropertyValuationResponseValuationType.TAX_ASSESSED]: 'Tax Assessed',
    [PropertyValuationResponseValuationType.PURCHASE]: 'Purchase',
    [PropertyValuationResponseValuationType.INSURANCE]: 'Insurance',
    [PropertyValuationResponseValuationType.USER_ESTIMATE]: 'User Estimate',
  };
  return labels[type];
};

export const formatFinancingType = (
  type: PropertyFinancingResponseFinancingType
): string => {
  const labels: Record<PropertyFinancingResponseFinancingType, string> = {
    [PropertyFinancingResponseFinancingType.MORTGAGE]: 'Mortgage',
    [PropertyFinancingResponseFinancingType.LEASING]: 'Leasing',
    [PropertyFinancingResponseFinancingType.LOAN]: 'Loan',
    [PropertyFinancingResponseFinancingType.LINE_OF_CREDIT]: 'Line of Credit',
    [PropertyFinancingResponseFinancingType.PRIVATE_FINANCING]:
      'Private Financing',
    [PropertyFinancingResponseFinancingType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatRateType = (
  type: PropertyFinancingResponseRateType
): string => {
  const labels: Record<PropertyFinancingResponseRateType, string> = {
    [PropertyFinancingResponseRateType.FIXED]: 'Fixed',
    [PropertyFinancingResponseRateType.VARIABLE]: 'Variable',
    [PropertyFinancingResponseRateType.INTEREST_ONLY]: 'Interest Only',
    [PropertyFinancingResponseRateType.HYBRID]: 'Hybrid',
  };
  return labels[type];
};

export const formatFinancingStatus = (
  status: PropertyFinancingResponseStatus
): string => {
  const labels: Record<PropertyFinancingResponseStatus, string> = {
    [PropertyFinancingResponseStatus.ACTIVE]: 'Active',
    [PropertyFinancingResponseStatus.PAID_OFF]: 'Paid Off',
    [PropertyFinancingResponseStatus.REFINANCED]: 'Refinanced',
    [PropertyFinancingResponseStatus.DEFAULTED]: 'Defaulted',
  };
  return labels[status];
};

export const formatPaymentStatus = (
  status: FinancingPaymentResponseStatus
): string => {
  const labels: Record<FinancingPaymentResponseStatus, string> = {
    [FinancingPaymentResponseStatus.SCHEDULED]: 'Scheduled',
    [FinancingPaymentResponseStatus.COMPLETED]: 'Completed',
    [FinancingPaymentResponseStatus.MISSED]: 'Missed',
    [FinancingPaymentResponseStatus.LATE]: 'Late',
  };
  return labels[status];
};

export const formatInsuranceType = (
  type: PropertyInsuranceResponseInsuranceType
): string => {
  const labels: Record<PropertyInsuranceResponseInsuranceType, string> = {
    [PropertyInsuranceResponseInsuranceType.BUILDING]: 'Building',
    [PropertyInsuranceResponseInsuranceType.LIABILITY]: 'Liability',
    [PropertyInsuranceResponseInsuranceType.CONTENTS]: 'Contents',
    [PropertyInsuranceResponseInsuranceType.FLOOD]: 'Flood',
    [PropertyInsuranceResponseInsuranceType.EARTHQUAKE]: 'Earthquake',
    [PropertyInsuranceResponseInsuranceType.UMBRELLA]: 'Umbrella',
    [PropertyInsuranceResponseInsuranceType.RENT_GUARANTEE]: 'Rent Guarantee',
    [PropertyInsuranceResponseInsuranceType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatInsuranceStatus = (
  status: PropertyInsuranceResponseStatus
): string => {
  const labels: Record<PropertyInsuranceResponseStatus, string> = {
    [PropertyInsuranceResponseStatus.ACTIVE]: 'Active',
    [PropertyInsuranceResponseStatus.EXPIRED]: 'Expired',
    [PropertyInsuranceResponseStatus.CANCELLED]: 'Cancelled',
  };
  return labels[status];
};

export const formatTaxType = (type: PropertyTaxResponseTaxType): string => {
  const labels: Record<PropertyTaxResponseTaxType, string> = {
    [PropertyTaxResponseTaxType.PROPERTY]: 'Property',
    [PropertyTaxResponseTaxType.MUNICIPAL]: 'Municipal',
    [PropertyTaxResponseTaxType.STATE]: 'State',
    [PropertyTaxResponseTaxType.LOCAL]: 'Local',
    [PropertyTaxResponseTaxType.LAND]: 'Land',
    [PropertyTaxResponseTaxType.SPECIAL_ASSESSMENT]: 'Special Assessment',
    [PropertyTaxResponseTaxType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatTaxStatus = (status: PropertyTaxResponseStatus): string => {
  const labels: Record<PropertyTaxResponseStatus, string> = {
    [PropertyTaxResponseStatus.ACTIVE]: 'Active',
    [PropertyTaxResponseStatus.EXPIRED]: 'Expired',
    [PropertyTaxResponseStatus.EXEMPT]: 'Exempt',
  };
  return labels[status];
};

export const formatFeeType = (type: PropertyFeeResponseFeeType): string => {
  const labels: Record<PropertyFeeResponseFeeType, string> = {
    [PropertyFeeResponseFeeType.HOA]: 'HOA',
    [PropertyFeeResponseFeeType.MANAGEMENT]: 'Management',
    [PropertyFeeResponseFeeType.MAINTENANCE_RESERVE]: 'Maintenance Reserve',
    [PropertyFeeResponseFeeType.CLEANING]: 'Cleaning',
    [PropertyFeeResponseFeeType.GARDENING]: 'Gardening',
    [PropertyFeeResponseFeeType.SECURITY]: 'Security',
    [PropertyFeeResponseFeeType.WASTE_MANAGEMENT]: 'Waste Management',
    [PropertyFeeResponseFeeType.WATER]: 'Water',
    [PropertyFeeResponseFeeType.UTILITIES]: 'Utilities',
    [PropertyFeeResponseFeeType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatFeeStatus = (status: PropertyFeeResponseStatus): string => {
  const labels: Record<PropertyFeeResponseStatus, string> = {
    [PropertyFeeResponseStatus.ACTIVE]: 'Active',
    [PropertyFeeResponseStatus.EXPIRED]: 'Expired',
    [PropertyFeeResponseStatus.CANCELLED]: 'Cancelled',
  };
  return labels[status];
};

export const formatPaymentFrequency = (freq: PaymentFrequency): string => {
  const labels: Record<PaymentFrequency, string> = {
    [PaymentFrequency.MONTHLY]: 'Monthly',
    [PaymentFrequency.QUARTERLY]: 'Quarterly',
    [PaymentFrequency.SEMI_ANNUALLY]: 'Semi-Annually',
    [PaymentFrequency.ANNUALLY]: 'Annually',
    [PaymentFrequency.CUSTOM]: 'Custom',
  };
  return labels[freq];
};

// ============================================================
// Short aliases (used by hooks / components)
// ============================================================

export type UpsertAcquisitionRequest = UpsertPropertyAcquisitionRequest;
export type CreateValuationRequest = CreatePropertyValuationRequest;
export type UpdateValuationRequest = UpdatePropertyValuationRequest;
export type CreateFinancingRequest = CreatePropertyFinancingRequest;
export type UpdateFinancingRequest = UpdatePropertyFinancingRequest;
export type CreateInsuranceRequest = CreatePropertyInsuranceRequest;
export type UpdateInsuranceRequest = UpdatePropertyInsuranceRequest;
export type CreateTaxRequest = CreatePropertyTaxRequest;
export type UpdateTaxRequest = UpdatePropertyTaxRequest;
export type CreateFeeRequest = CreatePropertyFeeRequest;
export type UpdateFeeRequest = UpdatePropertyFeeRequest;
