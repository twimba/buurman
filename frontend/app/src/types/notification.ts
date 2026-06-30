// Canonical response type comes from the generated spec. Its enum-like fields
// (notificationType, channel, status) are typed as `string` server-side.
export type { NotificationResponse } from '../generated/models';

// Frontend-only enums kept as VALUES for filter dropdowns, badges and label maps.
// They mirror the backend's string values, giving stronger typing in the UI layer.

export enum NotificationType {
  WELCOME = 'WELCOME',
  VERIFICATION_CODE = 'VERIFICATION_CODE',
  TEAM_INVITATION = 'TEAM_INVITATION',
  INVITATION_ACCEPTED = 'INVITATION_ACCEPTED',
  PASSWORD_CHANGED = 'PASSWORD_CHANGED',
  PAYMENT_REMINDER = 'PAYMENT_REMINDER',
  CONTRACT_EXPIRY = 'CONTRACT_EXPIRY',
  PROPERTY_CREATED = 'PROPERTY_CREATED',
  CONTRACT_CREATED = 'CONTRACT_CREATED',
  CONTRACT_STATUS_CHANGED = 'CONTRACT_STATUS_CHANGED',
  CONTRACT_REOPENED = 'CONTRACT_REOPENED',
  PAYMENT_PAID = 'PAYMENT_PAID',
  PAYMENT_RECEIVAL = 'PAYMENT_RECEIVAL',
  EXPENSE_CREATED = 'EXPENSE_CREATED',
}

export enum NotificationChannel {
  EMAIL = 'EMAIL',
  SMS = 'SMS',
}

export enum NotificationStatus {
  PENDING = 'PENDING',
  QUEUED = 'QUEUED',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  FAILED = 'FAILED',
  BOUNCED = 'BOUNCED',
  REJECTED = 'REJECTED',
  DEMO_BLOCKED = 'DEMO_BLOCKED',
}

// Frontend-only filter shape used by the admin notifications UI.
export interface NotificationFilterParams {
  type?: NotificationType;
  channel?: NotificationChannel;
  status?: NotificationStatus;
  recipientEmail?: string;
  dateFrom?: string;
  dateTo?: string;
}
