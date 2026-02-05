import { PropertySummary } from './property';
import { TenantSummary } from './tenant';

export enum ContractType {
  FIXED_TERM = 'FIXED_TERM',
  INDEFINITE = 'INDEFINITE',
  FURNISHED = 'FURNISHED',
  UNFURNISHED = 'UNFURNISHED',
}

export enum PaymentFrequency {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  ANNUALLY = 'ANNUALLY',
}

export enum ContractStatus {
  DRAFT = 'DRAFT',
  PENDING_SIGNATURE = 'PENDING_SIGNATURE',
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  TERMINATED = 'TERMINATED',
}

export interface ContractResponse {
  identifier: string;
  property: PropertySummary;
  tenant: TenantSummary;
  contractType: ContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  paymentDueDay?: number;
  autoRenewal: boolean;
  renewalNoticeDays?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  status: ContractStatus;
  termsAndConditions?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContractSummary {
  identifier: string;
  property: PropertySummary;
  tenant: TenantSummary;
  startDate: string;
  endDate?: string;
  rentAmount: number;
  status: ContractStatus;
}

export interface CreateContractRequest {
  propertyIdentifier: string;
  tenantIdentifier: string;
  contractType: ContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  currency?: string;
  paymentFrequency: PaymentFrequency;
  paymentDueDay?: number;
  autoRenewal?: boolean;
  renewalNoticeDays?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  termsAndConditions?: string;
  notes?: string;
}

export interface UpdateContractRequest {
  propertyIdentifier: string;
  tenantIdentifier: string;
  contractType: ContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  currency?: string;
  paymentFrequency: PaymentFrequency;
  paymentDueDay?: number;
  autoRenewal?: boolean;
  renewalNoticeDays?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  termsAndConditions?: string;
  notes?: string;
}

export interface ChangeContractStatusRequest {
  status: ContractStatus;
  reason?: string;
}
