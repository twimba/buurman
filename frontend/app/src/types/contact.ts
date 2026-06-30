// Generated model re-exports (thin layer). Frontend-only enums/label maps kept below.

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

// Enums used as values (generated `const` + `type` of the same name)
export {
  InteractionType,
  RelationshipType,
  ContactTag,
  ContactType,
} from '../generated/models';

// Generated response/request model types
export type {
  ContactResponse,
  ContactListItemResponse,
  ContactSummary,
  CreateContactRequest,
  UpdateContactRequest,
  ContactAddressResponse,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
  ContactNoteResponse,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
  ContactRelationshipResponse,
  CreateContactRelationshipRequest,
  UpdateContactRelationshipRequest,
  AddContactTagRequest,
  ContactActivityItem,
  ContactBalanceSummary,
  ContactPropertyAssignment,
  PropertyContactHistoryResponse,
  DuplicateCheckRequest,
  DuplicateCheckResponse,
  BalanceStatus,
} from '../generated/models';

// Generated name differs (DuplicateMatch -> DuplicateCheckMatch); contact is a ContactSummary
export type { DuplicateMatch as DuplicateCheckMatch } from '../generated/models';

import {
  InteractionType,
  RelationshipType,
  ContactTag,
  ContactType,
} from '../generated/models';

// --- Frontend-only label maps & color maps (UI concerns) ---

export const INTERACTION_TYPE_LABELS: Record<InteractionType, string> = {
  [InteractionType.PHONE_CALL]: 'Phone Call',
  [InteractionType.MEETING]: 'Meeting',
  [InteractionType.VIEWING]: 'Viewing',
  [InteractionType.KEY_HANDOVER]: 'Key Handover',
  [InteractionType.INSPECTION]: 'Inspection',
  [InteractionType.NOTE]: 'Note',
  [InteractionType.OTHER]: 'Other',
};

export const RELATIONSHIP_TYPE_LABELS: Record<RelationshipType, string> = {
  [RelationshipType.GUARANTOR_FOR]: 'Guarantor for',
  [RelationshipType.FAMILY_OF]: 'Family of',
  [RelationshipType.PARTNER_OF]: 'Partner of',
  [RelationshipType.WORKS_FOR]: 'Works for',
  [RelationshipType.CONTACT_PERSON_FOR]: 'Contact person for',
  [RelationshipType.OTHER]: 'Other',
};

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

export const CONTACT_TYPE_LABELS: Record<ContactType, string> = {
  [ContactType.INDIVIDUAL]: 'Individual',
  [ContactType.COMPANY]: 'Company',
  [ContactType.SERVICE_PROVIDER]: 'Service Provider',
};
