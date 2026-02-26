// ============================================================
// Property Financials — Enums
// ============================================================

// Property Acquisition
export enum AcquisitionType {
  PURCHASE = 'PURCHASE',
  INHERITANCE = 'INHERITANCE',
  GIFT = 'GIFT',
  FORECLOSURE = 'FORECLOSURE',
  AUCTION = 'AUCTION',
  OTHER = 'OTHER',
}

export enum DepreciationMethod {
  STRAIGHT_LINE = 'STRAIGHT_LINE',
  DECLINING_BALANCE = 'DECLINING_BALANCE',
  NONE = 'NONE',
}

// Property Valuation
export enum ValuationType {
  MARKET = 'MARKET',
  APPRAISAL = 'APPRAISAL',
  TAX_ASSESSED = 'TAX_ASSESSED',
  PURCHASE = 'PURCHASE',
  INSURANCE = 'INSURANCE',
  USER_ESTIMATE = 'USER_ESTIMATE',
}

// Property Financing
export enum FinancingType {
  MORTGAGE = 'MORTGAGE',
  LEASING = 'LEASING',
  LOAN = 'LOAN',
  LINE_OF_CREDIT = 'LINE_OF_CREDIT',
  PRIVATE_FINANCING = 'PRIVATE_FINANCING',
  OTHER = 'OTHER',
}

export enum RateType {
  FIXED = 'FIXED',
  VARIABLE = 'VARIABLE',
  INTEREST_ONLY = 'INTEREST_ONLY',
  HYBRID = 'HYBRID',
}

export enum FinancingStatus {
  ACTIVE = 'ACTIVE',
  PAID_OFF = 'PAID_OFF',
  REFINANCED = 'REFINANCED',
  DEFAULTED = 'DEFAULTED',
}

// Financing Payment
export enum PaymentStatus {
  SCHEDULED = 'SCHEDULED',
  COMPLETED = 'COMPLETED',
  MISSED = 'MISSED',
  LATE = 'LATE',
}

// Insurance
export enum InsuranceType {
  BUILDING = 'BUILDING',
  LIABILITY = 'LIABILITY',
  CONTENTS = 'CONTENTS',
  FLOOD = 'FLOOD',
  EARTHQUAKE = 'EARTHQUAKE',
  UMBRELLA = 'UMBRELLA',
  RENT_GUARANTEE = 'RENT_GUARANTEE',
  OTHER = 'OTHER',
}

export enum InsuranceStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

// Tax
export enum TaxType {
  PROPERTY = 'PROPERTY',
  MUNICIPAL = 'MUNICIPAL',
  STATE = 'STATE',
  LOCAL = 'LOCAL',
  LAND = 'LAND',
  SPECIAL_ASSESSMENT = 'SPECIAL_ASSESSMENT',
  OTHER = 'OTHER',
}

export enum TaxStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  EXEMPT = 'EXEMPT',
}

// Fee
export enum FeeType {
  HOA = 'HOA',
  MANAGEMENT = 'MANAGEMENT',
  MAINTENANCE_RESERVE = 'MAINTENANCE_RESERVE',
  CLEANING = 'CLEANING',
  GARDENING = 'GARDENING',
  SECURITY = 'SECURITY',
  WASTE_MANAGEMENT = 'WASTE_MANAGEMENT',
  WATER = 'WATER',
  UTILITIES = 'UTILITIES',
  OTHER = 'OTHER',
}

export enum FeeStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

// Payment Frequency (shared)
export enum PaymentFrequency {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  SEMI_ANNUALLY = 'SEMI_ANNUALLY',
  ANNUALLY = 'ANNUALLY',
  CUSTOM = 'CUSTOM',
}

// ============================================================
// Response interfaces
// ============================================================

export interface PropertyAcquisitionResponse {
  identifier: string;
  acquisitionType: AcquisitionType;
  acquisitionDate?: string;
  purchasePrice?: number;
  purchasePriceCurrency?: string;
  closingCosts?: number;
  closingCostsCurrency?: string;
  renovationCosts?: number;
  renovationCostsCurrency?: string;
  landValue?: number;
  landValueCurrency?: string;
  depreciationMethod?: DepreciationMethod;
  depreciationYears?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyValuationResponse {
  identifier: string;
  valuationType: ValuationType;
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
  financingType: FinancingType;
  rateType: RateType;
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
  status: FinancingStatus;
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
  status: PaymentStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyInsuranceResponse {
  identifier: string;
  propertyIdentifier: string;
  insuranceType: InsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium: number;
  annualPremiumCurrency: string;
  paymentFrequency: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status: InsuranceStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyTaxResponse {
  identifier: string;
  propertyIdentifier: string;
  taxType: TaxType;
  authority?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status: TaxStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyFeeResponse {
  identifier: string;
  propertyIdentifier: string;
  feeType: FeeType;
  name?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status: FeeStatus;
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
// Request interfaces
// ============================================================

export interface UpsertPropertyAcquisitionRequest {
  acquisitionType: AcquisitionType;
  acquisitionDate?: string;
  purchasePrice?: number;
  purchasePriceCurrency?: string;
  closingCosts?: number;
  closingCostsCurrency?: string;
  renovationCosts?: number;
  renovationCostsCurrency?: string;
  landValue?: number;
  landValueCurrency?: string;
  depreciationMethod?: DepreciationMethod;
  depreciationYears?: number;
  notes?: string;
}

export interface CreatePropertyValuationRequest {
  valuationType: ValuationType;
  valuationDate: string;
  amount: number;
  currency: string;
  source?: string;
  notes?: string;
}

export interface UpdatePropertyValuationRequest {
  valuationType?: ValuationType;
  valuationDate?: string;
  amount?: number;
  currency?: string;
  source?: string;
  notes?: string;
}

export interface CreatePropertyFinancingRequest {
  financingType: FinancingType;
  rateType?: RateType;
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
  status?: FinancingStatus;
  notes?: string;
}

export interface UpdatePropertyFinancingRequest {
  financingType?: FinancingType;
  rateType?: RateType;
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
  status?: FinancingStatus;
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
  status?: PaymentStatus;
  notes?: string;
}

export interface UpdateFinancingPaymentRequest {
  paymentDate?: string;
  totalAmount?: number;
  principalAmount?: number;
  interestAmount?: number;
  escrowAmount?: number;
  extraPayment?: number;
  currency?: string;
  status?: PaymentStatus;
  notes?: string;
}

export interface CreatePropertyInsuranceRequest {
  insuranceType: InsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium: number;
  annualPremiumCurrency: string;
  paymentFrequency?: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status?: InsuranceStatus;
  notes?: string;
}

export interface UpdatePropertyInsuranceRequest {
  insuranceType?: InsuranceType;
  provider?: string;
  policyNumber?: string;
  coverageAmount?: number;
  coverageAmountCurrency?: string;
  annualPremium?: number;
  annualPremiumCurrency?: string;
  paymentFrequency?: PaymentFrequency;
  startDate?: string;
  endDate?: string;
  status?: InsuranceStatus;
  notes?: string;
}

export interface CreatePropertyTaxRequest {
  taxType: TaxType;
  authority?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status?: TaxStatus;
  notes?: string;
}

export interface UpdatePropertyTaxRequest {
  taxType?: TaxType;
  authority?: string;
  annualAmount?: number;
  currency?: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  taxYear?: number;
  startDate?: string;
  endDate?: string;
  status?: TaxStatus;
  notes?: string;
}

export interface CreatePropertyFeeRequest {
  feeType: FeeType;
  name?: string;
  annualAmount: number;
  currency: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status?: FeeStatus;
  notes?: string;
}

export interface UpdatePropertyFeeRequest {
  feeType?: FeeType;
  name?: string;
  annualAmount?: number;
  currency?: string;
  paymentFrequency?: PaymentFrequency;
  dueMonths?: string;
  startDate?: string;
  endDate?: string;
  status?: FeeStatus;
  notes?: string;
}

// ============================================================
// Label formatters
// ============================================================

export const formatAcquisitionType = (type: AcquisitionType): string => {
  const labels: Record<AcquisitionType, string> = {
    [AcquisitionType.PURCHASE]: 'Purchase',
    [AcquisitionType.INHERITANCE]: 'Inheritance',
    [AcquisitionType.GIFT]: 'Gift',
    [AcquisitionType.FORECLOSURE]: 'Foreclosure',
    [AcquisitionType.AUCTION]: 'Auction',
    [AcquisitionType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatDepreciationMethod = (
  method: DepreciationMethod
): string => {
  const labels: Record<DepreciationMethod, string> = {
    [DepreciationMethod.STRAIGHT_LINE]: 'Straight Line',
    [DepreciationMethod.DECLINING_BALANCE]: 'Declining Balance',
    [DepreciationMethod.NONE]: 'None',
  };
  return labels[method];
};

export const formatValuationType = (type: ValuationType): string => {
  const labels: Record<ValuationType, string> = {
    [ValuationType.MARKET]: 'Market',
    [ValuationType.APPRAISAL]: 'Appraisal',
    [ValuationType.TAX_ASSESSED]: 'Tax Assessed',
    [ValuationType.PURCHASE]: 'Purchase',
    [ValuationType.INSURANCE]: 'Insurance',
    [ValuationType.USER_ESTIMATE]: 'User Estimate',
  };
  return labels[type];
};

export const formatFinancingType = (type: FinancingType): string => {
  const labels: Record<FinancingType, string> = {
    [FinancingType.MORTGAGE]: 'Mortgage',
    [FinancingType.LEASING]: 'Leasing',
    [FinancingType.LOAN]: 'Loan',
    [FinancingType.LINE_OF_CREDIT]: 'Line of Credit',
    [FinancingType.PRIVATE_FINANCING]: 'Private Financing',
    [FinancingType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatRateType = (type: RateType): string => {
  const labels: Record<RateType, string> = {
    [RateType.FIXED]: 'Fixed',
    [RateType.VARIABLE]: 'Variable',
    [RateType.INTEREST_ONLY]: 'Interest Only',
    [RateType.HYBRID]: 'Hybrid',
  };
  return labels[type];
};

export const formatFinancingStatus = (status: FinancingStatus): string => {
  const labels: Record<FinancingStatus, string> = {
    [FinancingStatus.ACTIVE]: 'Active',
    [FinancingStatus.PAID_OFF]: 'Paid Off',
    [FinancingStatus.REFINANCED]: 'Refinanced',
    [FinancingStatus.DEFAULTED]: 'Defaulted',
  };
  return labels[status];
};

export const formatPaymentStatus = (status: PaymentStatus): string => {
  const labels: Record<PaymentStatus, string> = {
    [PaymentStatus.SCHEDULED]: 'Scheduled',
    [PaymentStatus.COMPLETED]: 'Completed',
    [PaymentStatus.MISSED]: 'Missed',
    [PaymentStatus.LATE]: 'Late',
  };
  return labels[status];
};

export const formatInsuranceType = (type: InsuranceType): string => {
  const labels: Record<InsuranceType, string> = {
    [InsuranceType.BUILDING]: 'Building',
    [InsuranceType.LIABILITY]: 'Liability',
    [InsuranceType.CONTENTS]: 'Contents',
    [InsuranceType.FLOOD]: 'Flood',
    [InsuranceType.EARTHQUAKE]: 'Earthquake',
    [InsuranceType.UMBRELLA]: 'Umbrella',
    [InsuranceType.RENT_GUARANTEE]: 'Rent Guarantee',
    [InsuranceType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatInsuranceStatus = (status: InsuranceStatus): string => {
  const labels: Record<InsuranceStatus, string> = {
    [InsuranceStatus.ACTIVE]: 'Active',
    [InsuranceStatus.EXPIRED]: 'Expired',
    [InsuranceStatus.CANCELLED]: 'Cancelled',
  };
  return labels[status];
};

export const formatTaxType = (type: TaxType): string => {
  const labels: Record<TaxType, string> = {
    [TaxType.PROPERTY]: 'Property',
    [TaxType.MUNICIPAL]: 'Municipal',
    [TaxType.STATE]: 'State',
    [TaxType.LOCAL]: 'Local',
    [TaxType.LAND]: 'Land',
    [TaxType.SPECIAL_ASSESSMENT]: 'Special Assessment',
    [TaxType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatTaxStatus = (status: TaxStatus): string => {
  const labels: Record<TaxStatus, string> = {
    [TaxStatus.ACTIVE]: 'Active',
    [TaxStatus.EXPIRED]: 'Expired',
    [TaxStatus.EXEMPT]: 'Exempt',
  };
  return labels[status];
};

export const formatFeeType = (type: FeeType): string => {
  const labels: Record<FeeType, string> = {
    [FeeType.HOA]: 'HOA',
    [FeeType.MANAGEMENT]: 'Management',
    [FeeType.MAINTENANCE_RESERVE]: 'Maintenance Reserve',
    [FeeType.CLEANING]: 'Cleaning',
    [FeeType.GARDENING]: 'Gardening',
    [FeeType.SECURITY]: 'Security',
    [FeeType.WASTE_MANAGEMENT]: 'Waste Management',
    [FeeType.WATER]: 'Water',
    [FeeType.UTILITIES]: 'Utilities',
    [FeeType.OTHER]: 'Other',
  };
  return labels[type];
};

export const formatFeeStatus = (status: FeeStatus): string => {
  const labels: Record<FeeStatus, string> = {
    [FeeStatus.ACTIVE]: 'Active',
    [FeeStatus.EXPIRED]: 'Expired',
    [FeeStatus.CANCELLED]: 'Cancelled',
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
