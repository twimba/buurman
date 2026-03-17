export interface ContractExtensionResponse {
  identifier: string;
  contractIdentifier: string;
  extensionNumber: number;
  previousEndDate: string;
  newEndDate?: string;
  previousRentAmount: number;
  previousRentCurrency: string;
  newRentAmount: number;
  newRentCurrency: string;
  rentAdjustmentType: RentAdjustmentType;
  rentAdjustmentValue?: number;
  status: ExtensionStatus;
  triggerType: TriggerType;
  notes?: string;
  declinedReason?: string;
  activatedAt?: string;
  confirmedAt?: string;
  supersededAt?: string;
  createdAt: string;
}

export type ExtensionStatus =
  | 'DRAFT'
  | 'ACTIVE'
  | 'SUPERSEDED'
  | 'CANCELLED'
  | 'DECLINED';
export type TriggerType = 'MANUAL' | 'AUTO';
export type RentAdjustmentType =
  | 'NONE'
  | 'FIXED_PERCENTAGE'
  | 'FIXED_AMOUNT'
  | 'MANUAL';
export type RenewalMode = 'NONE' | 'AUTOMATIC' | 'MANUAL';
export type LandlordType = 'NATURAL_PERSON' | 'LEGAL_ENTITY';

export interface CreateContractExtensionRequest {
  newEndDate?: string;
  newRentAmount?: number;
  rentAdjustmentType?: RentAdjustmentType;
  rentAdjustmentValue?: number;
  notes?: string;
}

export interface DeclineContractExtensionRequest {
  reason?: string;
}

export interface UpcomingRenewalResponse {
  contractIdentifier: string;
  propertyName?: string;
  tenantName?: string;
  effectiveEndDate: string;
  renewalMode: RenewalMode;
  renewalTermMonths?: number;
  currentRentAmount: number;
  currency: string;
  daysUntilExpiry: number;
}

export interface JurisdictionDefaultResponse {
  countryCode: string;
  defaults: Record<string, string>;
  disclaimer: string;
}
