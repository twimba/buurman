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
}

export interface NotificationResponse {
  identifier: string;
  notificationType: NotificationType;
  channel: NotificationChannel;
  subject: string;
  body: string | null;
  recipientEmail: string;
  recipientPhone: string | null;
  status: NotificationStatus;
  providerStatus: string | null;
  providerError: string | null;
  openCount: number;
  clickCount: number;
  firstOpenedAt: string | null;
  firstClickedAt: string | null;
  resentFromIdentifier: string | null;
  resendReason: string | null;
  createdAt: string;
  statusUpdatedAt: string | null;
}

export interface NotificationStatsResponse {
  totalCount: number;
  pendingCount: number;
  sentCount: number;
  deliveredCount: number;
  failedCount: number;
  byChannel: Record<string, number>;
}

export interface NotificationFilterParams {
  type?: NotificationType;
  channel?: NotificationChannel;
  status?: NotificationStatus;
  recipientEmail?: string;
  dateFrom?: string;
  dateTo?: string;
}
