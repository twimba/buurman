import { PropertySummary } from './property';
import { ContactSummary, CreateContactRequest } from './contact';
import type {
  RenewalMode,
  RentAdjustmentType,
  LandlordType,
} from './contractExtension';

// Enums — re-exported from generated
export {
  ContractResponseContractType as ContractType,
  type ContractResponseContractType,
} from '../generated/models';

export {
  ContractResponsePaymentFrequency as PaymentFrequency,
  type ContractResponsePaymentFrequency,
} from '../generated/models';

export {
  ContractResponseStatus as ContractStatus,
  type ContractResponseStatus,
} from '../generated/models';

export {
  ContractPartyResponseRole as ContractPartyRole,
  type ContractPartyResponseRole,
} from '../generated/models';

export type { EnumValue } from '../generated/models';
export type { ValidationSchema } from '../generated/models';
export type { CountryMetadataSchemaResponse } from '../generated/models';

export {
  RentComponentType,
  type RentComponentType as RentComponentTypeValue,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import type { ContractResponseContractType } from '../generated/models';
import type { ContractResponsePaymentFrequency } from '../generated/models';
import type { ContractResponseStatus } from '../generated/models';
import { ContractPartyResponseRole } from '../generated/models';
import { RentComponentType } from '../generated/models';

export const PARTY_ROLE_LABELS: Record<ContractPartyResponseRole, string> = {
  [ContractPartyResponseRole.PRIMARY_TENANT]: 'Primary Contact',
  [ContractPartyResponseRole.GUARANTOR]: 'Guarantor',
  [ContractPartyResponseRole.COSIGNER]: 'Co-signer',
  [ContractPartyResponseRole.EXTRA_TENANT]: 'Additional Contact',
};

export const RENT_COMPONENT_LABELS: Record<RentComponentType, string> = {
  [RentComponentType.BASE_RENT]: 'Base Rent',
  [RentComponentType.UTILITIES_ADVANCE]: 'Utilities Advance',
  [RentComponentType.SERVICE_COSTS]: 'Service Costs',
  [RentComponentType.HOA_FEES]: 'HOA / Condo Fees',
  [RentComponentType.FURNITURE_RENTAL]: 'Furniture Rental',
  [RentComponentType.PARKING]: 'Parking',
  [RentComponentType.STORAGE]: 'Storage',
  [RentComponentType.GARBAGE_COLLECTION]: 'Garbage Collection',
  [RentComponentType.OTHER]: 'Other',
};

export interface RentComponentFormItem {
  componentType: RentComponentType;
  amount: number | '';
  description?: string;
}

export interface RentComponentResponseItem {
  identifier: string;
  componentType: RentComponentType;
  componentTypeDisplayName: string;
  amount: number;
  currency: string;
  description?: string;
  sortOrder: number;
}

export interface ContractPartyResponse {
  identifier: string;
  contact: ContactSummary;
  role: ContractPartyResponseRole;
}

export interface ContractPartyRequest {
  contactIdentifier?: string;
  newContact?: CreateContactRequest;
  role: ContractPartyResponseRole;
}

export interface ContractResponse {
  identifier: string;
  property: PropertySummary;
  parties: ContractPartyResponse[];
  primaryContact: ContactSummary;
  contractType: ContractResponseContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  rentAmountCurrency: string;
  depositAmountCurrency?: string;
  securityDepositCurrency?: string;
  paymentFrequency: ContractResponsePaymentFrequency;
  paymentDueDay?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  status: ContractResponseStatus;
  termsAndConditions?: string;
  notes?: string;
  countryCode?: string;
  countryMetadata?: Record<string, unknown>;
  effectiveEndDate?: string;
  renewalMode?: RenewalMode;
  renewalTermMonths?: number;
  maxRenewals?: number;
  landlordNoticeDays?: number;
  contactNoticeDays?: number;
  requiresContactConfirmation?: boolean;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  landlordType?: LandlordType;
  regionCode?: string;
  extensionCount?: number;
  extensionsRemaining?: number;
  rentComponents: RentComponentResponseItem[];
  createdAt: string;
  updatedAt: string;
}

export interface ContractSummary {
  identifier: string;
  property: PropertySummary;
  primaryContact: ContactSummary;
  startDate: string;
  endDate?: string;
  rentAmount: number;
  status: ContractResponseStatus;
}

export interface CreateContractRequest {
  propertyIdentifier: string;
  parties: ContractPartyRequest[];
  contractType: ContractResponseContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  rentAmountCurrency?: string;
  depositAmountCurrency?: string;
  securityDepositCurrency?: string;
  paymentFrequency: ContractResponsePaymentFrequency;
  paymentDueDay?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  termsAndConditions?: string;
  notes?: string;
  countryMetadata?: Record<string, unknown>;
  renewalMode?: RenewalMode;
  renewalTermMonths?: number;
  maxRenewals?: number;
  landlordNoticeDays?: number;
  contactNoticeDays?: number;
  requiresContactConfirmation?: boolean;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  landlordType?: LandlordType;
  regionCode?: string;
  rentComponents?: RentComponentFormItem[];
}

export interface UpdateContractRequest {
  propertyIdentifier: string;
  contractType: ContractResponseContractType;
  startDate: string;
  endDate?: string;
  signedDate?: string;
  rentAmount: number;
  depositAmount?: number;
  securityDeposit?: number;
  rentAmountCurrency?: string;
  depositAmountCurrency?: string;
  securityDepositCurrency?: string;
  paymentFrequency: ContractResponsePaymentFrequency;
  paymentDueDay?: number;
  terminationNoticeDays?: number;
  lateFeePercentage?: number;
  termsAndConditions?: string;
  notes?: string;
  countryMetadata?: Record<string, unknown>;
  renewalMode?: RenewalMode;
  renewalTermMonths?: number;
  maxRenewals?: number;
  landlordNoticeDays?: number;
  contactNoticeDays?: number;
  requiresContactConfirmation?: boolean;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  landlordType?: LandlordType;
  regionCode?: string;
  rentComponents?: RentComponentFormItem[];
}

export interface RentPeriodResponse {
  identifier: string;
  rentAmount: number;
  effectiveFrom: string;
  effectiveTo?: string;
  notes?: string;
  percentageChange?: number;
  createdAt: string;
}

export interface MetadataFieldSchema {
  name: string;
  label: string;
  type: 'STRING' | 'INTEGER' | 'DECIMAL' | 'MONEY' | 'BOOLEAN' | 'ENUM';
  required: boolean;
  enumValues: { value: string; label: string }[];
  validation: { min?: number; max?: number; pattern?: string };
  group: string;
  helpText?: string;
  unit?: string;
}

export interface MetadataGroupSchema {
  key: string;
  label: string;
}

// Request interfaces — manual (generated adds to all optional fields)

export interface AddContractPartyRequest {
  contactIdentifier?: string;
  newContact?: CreateContactRequest;
  role: ContractPartyResponseRole;
}

export interface ChangePrimaryContactRequest {
  contactIdentifier?: string;
  newContact?: CreateContactRequest;
}

export interface ChangeContractStatusRequest {
  status: ContractResponseStatus;
  reason?: string;
}

export interface CreateRentPeriodRequest {
  rentAmount: number;
  effectiveFrom: string;
  notes?: string;
}

export interface UpdateRentPeriodRequest {
  rentAmount: number;
  effectiveFrom: string;
  notes?: string;
}

export interface CountryMetadataSchema {
  countryCode: string;
  countryName: string;
  hasDedicatedSchema: boolean;
  fields: MetadataFieldSchema[];
  groups: MetadataGroupSchema[];
}
