export interface BackofficeTeam {
  identifier: string;
  teamName: string;
  memberCount: number;
  ownerEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface BackofficeUser {
  identifier: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  emailVerified: boolean;
  disabled: boolean;
  teamCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface BackofficeNotification {
  identifier: string;
  teamIdentifier: string;
  teamName: string;
  notificationType: string;
  channel: string;
  subject: string;
  body: string;
  recipientEmail: string | null;
  recipientPhone: string | null;
  status: string;
  providerStatus: string | null;
  providerError: string | null;
  resentFromIdentifier: string | null;
  resendReason: string | null;
  createdAt: string;
  statusUpdatedAt: string | null;
}

export interface BackofficeDashboardStats {
  totalTeams: number;
  totalUsers: number;
  disabledUsers: number;
  totalNotifications: number;
  pendingNotifications: number;
  failedNotifications: number;
  deliveredNotifications: number;
  notificationsByChannel: Record<string, number>;
}

export interface NotificationStats {
  totalCount: number;
  pendingCount: number;
  sentCount: number;
  deliveredCount: number;
  failedCount: number;
  byChannel: Record<string, number>;
}
