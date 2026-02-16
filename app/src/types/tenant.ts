import { ContractPartyRole } from './contract';
import { PropertySummary } from './property';

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
  updatedAt: string;
}

export interface TenantSummary {
  identifier: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

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

export enum PropertyTenantActionType {
  LINKED = 'LINKED',
  UNLINKED = 'UNLINKED',
}

export interface PropertyTenantHistoryResponse {
  property: PropertySummary;
  movedInAt?: string;
  movedOutAt?: string;
  actionType: PropertyTenantActionType;
  performedBy: string;
  performedAt: string;
}

export enum AddressType {
  CURRENT = 'CURRENT',
  MAILING = 'MAILING',
  RELATIVE = 'RELATIVE',
  WORK = 'WORK',
  HISTORIC = 'HISTORIC',
}

export enum AddressStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
}

export interface TenantAddressResponse {
  identifier: string;
  street: string;
  city: string;
  postalCode?: string;
  country: string;
  addressType: AddressType;
  status: AddressStatus;
  latitude?: number | null;
  longitude?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  country: string;
  addressType: AddressType;
  status?: AddressStatus;
  latitude?: number | null;
  longitude?: number | null;
}

export interface UpdateTenantAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  country: string;
  addressType: AddressType;
  status: AddressStatus;
  latitude?: number | null;
  longitude?: number | null;
}
