// ============================================================
// Thin re-export of generated property-financials models.
// Frontend-only content kept: PaymentFrequency enum (spec uses
// `string` for this field), label formatters, short aliases.
// ============================================================

// --- Response / request models (thin re-export) ---
export type {
  PropertyFinancialSummaryResponse,
  PropertyAcquisitionResponse,
  UpsertPropertyAcquisitionRequest,
  PropertyValuationResponse,
  CreatePropertyValuationRequest,
  UpdatePropertyValuationRequest,
  PropertyFinancingResponse,
  CreatePropertyFinancingRequest,
  UpdatePropertyFinancingRequest,
  FinancingPaymentResponse,
  CreateFinancingPaymentRequest,
  UpdateFinancingPaymentRequest,
  PropertyInsuranceResponse,
  CreatePropertyInsuranceRequest,
  UpdatePropertyInsuranceRequest,
  PropertyTaxResponse,
  CreatePropertyTaxRequest,
  UpdatePropertyTaxRequest,
  PropertyFeeResponse,
  CreatePropertyFeeRequest,
  UpdatePropertyFeeRequest,
} from '../generated/models';

// --- Enums re-exported from generated (used as values + types) ---
export {
  PropertyAcquisitionResponseAcquisitionType as AcquisitionType,
  type PropertyAcquisitionResponseAcquisitionType,
} from '../generated/models';

export {
  PropertyAcquisitionResponseDepreciationMethod as DepreciationMethod,
  type PropertyAcquisitionResponseDepreciationMethod,
} from '../generated/models';

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

// PaymentFrequency — frontend-only. Generated models type this field as
// `string`; keep a real enum for dropdowns/form state and full member set
// (includes SEMI_ANNUALLY, CUSTOM).
export enum PaymentFrequency {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  SEMI_ANNUALLY = 'SEMI_ANNUALLY',
  ANNUALLY = 'ANNUALLY',
  CUSTOM = 'CUSTOM',
}

// ============================================================
// Imports for label formatters
// ============================================================

import { PropertyAcquisitionResponseAcquisitionType } from '../generated/models';
import { PropertyAcquisitionResponseDepreciationMethod } from '../generated/models';
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
      [PropertyAcquisitionResponseDepreciationMethod.STRAIGHT_LINE]:
        'Straight Line',
      [PropertyAcquisitionResponseDepreciationMethod.DECLINING_BALANCE]:
        'Declining Balance',
      [PropertyAcquisitionResponseDepreciationMethod.NONE]: 'None',
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

import type {
  UpsertPropertyAcquisitionRequest,
  CreatePropertyValuationRequest,
  UpdatePropertyValuationRequest,
  CreatePropertyFinancingRequest,
  UpdatePropertyFinancingRequest,
  CreatePropertyInsuranceRequest,
  UpdatePropertyInsuranceRequest,
  CreatePropertyTaxRequest,
  UpdatePropertyTaxRequest,
  CreatePropertyFeeRequest,
  UpdatePropertyFeeRequest,
} from '../generated/models';

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
