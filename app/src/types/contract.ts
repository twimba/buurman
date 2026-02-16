import { PropertySummary } from './property';
import { TenantSummary, CreateTenantRequest } from './tenant';

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

export enum ContractPartyRole {
  PRIMARY_TENANT = 'PRIMARY_TENANT',
  GUARANTOR = 'GUARANTOR',
  COSIGNER = 'COSIGNER',
  EXTRA_TENANT = 'EXTRA_TENANT',
}

export const PARTY_ROLE_LABELS: Record<ContractPartyRole, string> = {
  [ContractPartyRole.PRIMARY_TENANT]: 'Primary Tenant',
  [ContractPartyRole.GUARANTOR]: 'Guarantor',
  [ContractPartyRole.COSIGNER]: 'Co-signer',
  [ContractPartyRole.EXTRA_TENANT]: 'Additional Tenant',
};

export interface ContractPartyResponse {
  identifier: string;
  tenant: TenantSummary;
  role: ContractPartyRole;
}

export interface ContractPartyRequest {
  tenantIdentifier?: string;
  newTenant?: CreateTenantRequest;
  role: ContractPartyRole;
}

export interface AddContractPartyRequest {
  tenantIdentifier?: string;
  newTenant?: CreateTenantRequest;
  role: ContractPartyRole;
}

export interface ChangePrimaryTenantRequest {
  tenantIdentifier?: string;
  newTenant?: CreateTenantRequest;
}

export interface ContractResponse {
  identifier: string;
  property: PropertySummary;
  parties: ContractPartyResponse[];
  primaryTenant: TenantSummary;
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
  primaryTenant: TenantSummary;
  startDate: string;
  endDate?: string;
  rentAmount: number;
  status: ContractStatus;
}

export interface CreateContractRequest {
  propertyIdentifier: string;
  parties: ContractPartyRequest[];
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
