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
  currency: string | null;
  propertyStatusDistribution: Record<string, number>;
  propertyCategoryDistribution: Record<string, number>;
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
  online: boolean;
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
  openCount: number;
  clickCount: number;
  firstOpenedAt: string | null;
  firstClickedAt: string | null;
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

export interface LoggerConfiguration {
  name: string;
  configuredLevel: string | null;
  effectiveLevel: string;
}

export interface Buurmy {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string | null;
  lastLogin: string | null;
  requiredActions: string[];
}

export interface CreateBuurmyRequest {
  email: string;
  username?: string;
  firstName: string;
  lastName: string;
  password: string;
  temporaryPassword: boolean;
}

// System Info types
export type ServiceHealthStatus = "UP" | "DOWN" | "DISABLED" | "UNKNOWN";

export interface ServiceHealth {
  name: string;
  status: ServiceHealthStatus;
  latencyMs: number | null;
  details: string | null;
  error: string | null;
}

export interface MigrationEntry {
  version: string | null;
  description: string;
  state: string;
  installedOn: string | null;
  executionTimeMs: number | null;
  script: string;
}

export interface GcInfo {
  name: string;
  collectionCount: number;
  collectionTimeMs: number;
}

export interface ConfigEntry {
  category: string;
  key: string;
  value: string;
}

export interface MetricEntry {
  name: string;
  type: string;
  value: number;
  tags: Record<string, string>;
}

export interface HttpLatencyStats {
  meanMs: number;
  minMs: number;
  maxMs: number;
  p50Ms: number;
  p75Ms: number;
  p95Ms: number;
  p99Ms: number;
}

export interface MetricsSnapshot {
  httpRequestCount: number;
  httpRequestTotalTimeSeconds: number;
  httpLatency: HttpLatencyStats;
  custom: MetricEntry[];
}

export interface SessionInfo {
  appActiveUsers: number;
  backofficeActiveUsers: number;
}

export interface SystemInfoResponse {
  build: {
    version: string;
    gitCommit: string | null;
    gitCommitFull: string | null;
    gitBranch: string | null;
    gitCommitTime: string | null;
    gitDirty: boolean;
    buildTime: string | null;
  };
  runtime: {
    javaVersion: string;
    springBootVersion: string;
    activeProfiles: string;
    uptimeMs: number;
    heapUsedBytes: number;
    heapMaxBytes: number;
    nonHeapUsedBytes: number;
    nonHeapMaxBytes: number;
    cpuUsage: number;
    availableProcessors: number;
    threadCount: number;
    peakThreadCount: number;
    daemonThreadCount: number;
    garbageCollectors: GcInfo[];
    pid: number;
    serverTime: string;
  };
  migrations: {
    currentVersion: string | null;
    appliedCount: number;
    pendingCount: number;
    failedCount: number;
    entries: MigrationEntry[];
  };
  services: ServiceHealth[];
  configuration: ConfigEntry[];
  metrics: MetricsSnapshot;
  sessions: SessionInfo;
}
