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

// --- Contact sub-resource enums ---

export enum InteractionType {
  PHONE_CALL = 'PHONE_CALL',
  MEETING = 'MEETING',
  VIEWING = 'VIEWING',
  KEY_HANDOVER = 'KEY_HANDOVER',
  INSPECTION = 'INSPECTION',
  NOTE = 'NOTE',
  OTHER = 'OTHER',
}

export const INTERACTION_TYPE_LABELS: Record<InteractionType, string> = {
  [InteractionType.PHONE_CALL]: 'Phone Call',
  [InteractionType.MEETING]: 'Meeting',
  [InteractionType.VIEWING]: 'Viewing',
  [InteractionType.KEY_HANDOVER]: 'Key Handover',
  [InteractionType.INSPECTION]: 'Inspection',
  [InteractionType.NOTE]: 'Note',
  [InteractionType.OTHER]: 'Other',
};

export enum RelationshipType {
  GUARANTOR_FOR = 'GUARANTOR_FOR',
  FAMILY_OF = 'FAMILY_OF',
  PARTNER_OF = 'PARTNER_OF',
  WORKS_FOR = 'WORKS_FOR',
  CONTACT_PERSON_FOR = 'CONTACT_PERSON_FOR',
  OTHER = 'OTHER',
}

export const RELATIONSHIP_TYPE_LABELS: Record<RelationshipType, string> = {
  [RelationshipType.GUARANTOR_FOR]: 'Guarantor for',
  [RelationshipType.FAMILY_OF]: 'Family of',
  [RelationshipType.PARTNER_OF]: 'Partner of',
  [RelationshipType.WORKS_FOR]: 'Works for',
  [RelationshipType.CONTACT_PERSON_FOR]: 'Contact person for',
  [RelationshipType.OTHER]: 'Other',
};

export enum ContactTag {
  VIP = 'VIP',
  PROSPECT = 'PROSPECT',
  LATE_PAYER = 'LATE_PAYER',
  LONG_TERM = 'LONG_TERM',
  KEY_HOLDER = 'KEY_HOLDER',
  DO_NOT_CONTACT = 'DO_NOT_CONTACT',
  FORMER_TENANT = 'FORMER_TENANT',
  REFERRED = 'REFERRED',
}

export const CONTACT_TAG_LABELS: Record<ContactTag, string> = {
  [ContactTag.VIP]: 'VIP',
  [ContactTag.PROSPECT]: 'Prospect',
  [ContactTag.LATE_PAYER]: 'Late Payer',
  [ContactTag.LONG_TERM]: 'Long Term',
  [ContactTag.KEY_HOLDER]: 'Key Holder',
  [ContactTag.DO_NOT_CONTACT]: 'Do Not Contact',
  [ContactTag.FORMER_TENANT]: 'Former Tenant',
  [ContactTag.REFERRED]: 'Referred',
};

export type TagColorVariant =
  'gray' | 'red' | 'amber' | 'green' | 'teal' | 'blue' | 'purple' | 'rose';

export const TAG_COLORS: Record<ContactTag, TagColorVariant> = {
  [ContactTag.VIP]: 'purple',
  [ContactTag.PROSPECT]: 'blue',
  [ContactTag.LATE_PAYER]: 'red',
  [ContactTag.LONG_TERM]: 'green',
  [ContactTag.KEY_HOLDER]: 'amber',
  [ContactTag.DO_NOT_CONTACT]: 'rose',
  [ContactTag.FORMER_TENANT]: 'gray',
  [ContactTag.REFERRED]: 'teal',
};

// --- Contact sub-resource response types ---

export interface ContactNoteResponse {
  identifier: string;
  interactionType: InteractionType;
  subject?: string;
  body: string;
  occurredAt: string;
  followUpDate?: string;
  followUpReminderSent: boolean;
  pinned: boolean;
  createdByName?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ContactRelationshipResponse {
  identifier: string;
  relatedContact: ContactSummary;
  relationshipType: RelationshipType;
  displayLabel: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ContactActivityItem {
  eventType: string;
  occurredAt: string;
  description: string;
  relatedEntityIdentifier?: string;
  relatedEntityType?: string;
  noteIdentifier?: string;
  interactionType?: InteractionType;
  noteBody?: string;
  noteSubject?: string;
  pinned?: boolean;
  createdByName?: string;
}

// --- Contact sub-resource request types ---

export interface CreateContactNoteRequest {
  interactionType: InteractionType;
  subject?: string;
  body: string;
  occurredAt: string;
  followUpDate?: string;
}

export interface UpdateContactNoteRequest {
  interactionType: InteractionType;
  subject?: string;
  body: string;
  occurredAt: string;
  followUpDate?: string;
}

export interface CreateContactRelationshipRequest {
  targetContactIdentifier: string;
  relationshipType: RelationshipType;
  notes?: string;
}

export interface UpdateContactRelationshipRequest {
  relationshipType: RelationshipType;
  notes?: string;
}

export interface AddContactTagRequest {
  tag: ContactTag;
}

export type BalanceStatus = 'NONE' | 'ALL_PAID' | 'PENDING' | 'OVERDUE';

export interface ContactBalanceSummary {
  outstandingAmount: number;
  currency: string;
  status: BalanceStatus;
  outstandingPaymentCount: number;
  guaranteedAmount?: number;
  guaranteedPaymentCount?: number;
}

export interface ContactPropertyAssignment {
  property: PropertySummary;
  role?: ContractPartyRole;
}

export interface ContactListItemResponse {
  identifier: string;
  contactType: ContactType;
  displayName: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  companyName?: string;
  mainPhotoThumbnailUrl?: string;
  tags: ContactTag[];
  activeContractCount: number;
  balanceSummary?: ContactBalanceSummary;
  dataRetentionStatus?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ContactResponse {
  identifier: string;
  contactType: ContactType;
  displayName?: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  companyName?: string;
  tradeName?: string;
  industry?: string;
  invoiceEmail?: string;
  website?: string;
  dateOfBirth?: string;
  idExpiryDate?: string;
  notes?: string;
  tags: ContactTag[];
  dataRetentionStatus?: string;
  mainPhotoUrl?: string;
  mainPhotoThumbnailUrl?: string;
  activeProperties?: ContactPropertyAssignment[];
  balanceSummary?: ContactBalanceSummary;
  createdAt: string;
  updatedAt?: string;
}

export interface ContactSummary {
  identifier: string;
  contactType: ContactType;
  displayName: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  dataRetentionStatus: 'ACTIVE' | 'RETENTION_REQUESTED' | 'ANONYMIZED';
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

export type ContactType = 'INDIVIDUAL' | 'COMPANY' | 'SERVICE_PROVIDER';

export const CONTACT_TYPE_LABELS: Record<ContactType, string> = {
  INDIVIDUAL: 'Individual',
  COMPANY: 'Company',
  SERVICE_PROVIDER: 'Service Provider',
};

export interface CreateContactRequest {
  contactType: ContactType;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  idExpiryDate?: string;
  dateOfBirth?: string;
  companyName?: string;
  tradeName?: string;
  industry?: string;
  website?: string;
  invoiceEmail?: string;
  notes?: string;
  tags?: ContactTag[];
}

export interface UpdateContactRequest {
  contactType: ContactType;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  taxNumber?: string;
  idNumber?: string;
  idExpiryDate?: string;
  dateOfBirth?: string;
  companyName?: string;
  tradeName?: string;
  industry?: string;
  website?: string;
  invoiceEmail?: string;
  notes?: string;
  tags?: ContactTag[];
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

export interface DuplicateCheckRequest {
  firstName?: string;
  lastName?: string;
  companyName?: string;
  email?: string;
  phone?: string;
}

export interface DuplicateCheckMatch {
  contact: {
    identifier: string;
    firstName: string;
    lastName?: string;
    email?: string;
    phone?: string;
  };
  matchField: string;
  matchType: string;
}

export interface DuplicateCheckResponse {
  matches: DuplicateCheckMatch[];
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
