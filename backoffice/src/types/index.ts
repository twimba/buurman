export interface BackofficeTeam {
  identifier: string;
  teamName: string;
  memberCount: number;
  ownerEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface BackofficeTeamDetail {
  identifier: string;
  teamName: string;
  createdAt: string;
  updatedAt: string;
  members: TeamMemberInfo[];
  dataCounts: DataCounts;
  financialSnapshot: FinancialSnapshot;
  settings: TeamSettingsInfo;
}

export interface TeamMemberInfo {
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  role: string;
  isOwner: boolean;
  joinedAt: string | null;
  disabled: boolean;
}

export interface DataCounts {
  properties: number;
  tenants: number;
  contracts: number;
  expenses: number;
  payments: number;
  documents: number;
}

export interface FinancialSnapshot {
  totalActiveRent: number;
  currency: string;
  propertyStatusDistribution: Record<string, number>;
  contractStatusDistribution: Record<string, number>;
  paymentStatusDistribution: Record<string, number>;
}

export interface TeamSettingsInfo {
  paymentsAheadCount: number;
  autoGenerationEnabled: boolean;
  defaultCurrency: string;
  defaultCountry: string;
  timezone: string;
  dateFormat: string;
  fiscalYearStartMonth: string;
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

export interface ScheduledJob {
  jobName: string;
  jobGroup: string;
  jobClass: string;
  triggerName: string | null;
  triggerGroup: string | null;
  triggerType: "cron" | "simple" | null;
  scheduleExpression: string | null;
  triggerState: string;
  nextFireTime: string | null;
  previousFireTime: string | null;
}

export interface JobExecutionHistory {
  id: string;
  jobName: string;
  jobGroup: string;
  startedAt: string;
  endedAt: string | null;
  durationMs: number | null;
  status: "RUNNING" | "SUCCESS" | "FAILED";
  errorMessage: string | null;
  nodeId: string | null;
}
