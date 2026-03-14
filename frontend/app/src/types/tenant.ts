import { ContractPartyRole } from './contract';
import { PropertySummary } from './property';

// Enums — re-exported from generated
export {
  PropertyTenantHistoryResponseActionType as PropertyTenantActionType,
  type PropertyTenantHistoryResponseActionType,
} from '../generated/models';

export {
  TenantAddressResponseAddressType as AddressType,
  type TenantAddressResponseAddressType,
} from '../generated/models';

export {
  TenantAddressResponseStatus as AddressStatus,
  type TenantAddressResponseStatus,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import type { TenantAddressResponseAddressType } from '../generated/models';
import type { TenantAddressResponseStatus } from '../generated/models';
import type { PropertyTenantHistoryResponseActionType } from '../generated/models';

export interface TenantPropertyAssignment {
  property: PropertySummary;
  role?: ContractPartyRole;
}

export interface TenantResponse {
  identifier: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
  mainPhotoUrl?: string;
  mainPhotoThumbnailUrl?: string;
  activeProperties?: TenantPropertyAssignment[];
  createdAt: string;
  updatedAt?: string;
}

export interface TenantSummary {
  identifier: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

export interface PropertyTenantHistoryResponse {
  property: PropertySummary;
  movedInAt?: string;
  movedOutAt?: string;
  actionType: PropertyTenantHistoryResponseActionType;
  performedBy: string;
  performedAt: string;
}

// Request interfaces — manual (generated adds to all optional fields)

export interface CreateTenantRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface UpdateTenantRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface LinkTenantToPropertyRequest {
  propertyIdentifier: string;
  movedInAt?: string;
}

export interface CreateTenantAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: TenantAddressResponseAddressType;
  status?: TenantAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
}

export interface UpdateTenantAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: TenantAddressResponseAddressType;
  status: TenantAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
}

export interface TenantAddressResponse {
  identifier: string;
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: TenantAddressResponseAddressType;
  status: TenantAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
  createdAt: string;
  updatedAt: string;
}
