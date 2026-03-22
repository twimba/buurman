import { ContractPartyRole } from './contract';
import { PropertySummary } from './property';

// Enums — re-exported from generated
export {
  PropertyContactHistoryResponseActionType as PropertyContactActionType,
  type PropertyContactHistoryResponseActionType,
} from '../generated/models';

export {
  ContactAddressResponseAddressType as AddressType,
  type ContactAddressResponseAddressType,
} from '../generated/models';

export {
  ContactAddressResponseStatus as AddressStatus,
  type ContactAddressResponseStatus,
} from '../generated/models';

// Interfaces — kept manual (generated adds to optional fields)

import type { ContactAddressResponseAddressType } from '../generated/models';
import type { ContactAddressResponseStatus } from '../generated/models';
import type { PropertyContactHistoryResponseActionType } from '../generated/models';

export interface ContactPropertyAssignment {
  property: PropertySummary;
  role?: ContractPartyRole;
}

export interface ContactResponse {
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
  activeProperties?: ContactPropertyAssignment[];
  createdAt: string;
  updatedAt?: string;
}

export interface ContactSummary {
  identifier: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

export interface PropertyContactHistoryResponse {
  property: PropertySummary;
  movedInAt?: string;
  movedOutAt?: string;
  actionType: PropertyContactHistoryResponseActionType;
  performedBy: string;
  performedAt: string;
}

// Request interfaces — manual (generated adds to all optional fields)

export interface CreateContactRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface UpdateContactRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  additionalInfo?: string;
}

export interface LinkContactToPropertyRequest {
  propertyIdentifier: string;
  movedInAt?: string;
}

export interface CreateContactAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: ContactAddressResponseAddressType;
  status?: ContactAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
}

export interface UpdateContactAddressRequest {
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: ContactAddressResponseAddressType;
  status: ContactAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
}

export interface ContactAddressResponse {
  identifier: string;
  street: string;
  city: string;
  postalCode?: string;
  countryCode: string;
  addressType: ContactAddressResponseAddressType;
  status: ContactAddressResponseStatus;
  latitude?: number;
  longitude?: number;
  geocodeAccuracy?: string;
  createdAt: string;
  updatedAt: string;
}
