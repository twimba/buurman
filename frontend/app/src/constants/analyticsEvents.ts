export const AnalyticsEvent = {
  PROPERTY_CREATED: 'property_created',
  PROPERTY_VIEWED: 'property_viewed',
  PROPERTY_DELETED: 'property_deleted',
  CONTACT_CREATED: 'contact_created',
  CONTACT_VIEWED: 'contact_viewed',
  CONTACT_DELETED: 'contact_deleted',
  CONTRACT_CREATED: 'contract_created',
  CONTRACT_VIEWED: 'contract_viewed',
  CONTRACT_STATUS_CHANGED: 'contract_status_changed',
  PAYMENT_CREATED: 'payment_created',
  PAYMENT_MARKED_PAID: 'payment_marked_paid',
  PAYMENTS_GENERATED: 'payments_generated',
  EXPENSE_CREATED: 'expense_created',
  DOCUMENT_UPLOADED: 'document_uploaded',
  PHOTO_UPLOADED: 'photo_uploaded',
  TEAM_MEMBER_INVITED: 'team_member_invited',
  TEAM_SWITCHED: 'team_switched',
} as const;

export type AnalyticsEventName =
  (typeof AnalyticsEvent)[keyof typeof AnalyticsEvent];
