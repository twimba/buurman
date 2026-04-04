export interface BackofficeTeam {
  identifier: string;
  teamName: string;
  demo: boolean;
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
  userIdentifier?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  role: string;
  isOwner: boolean;
  joinedAt?: string;
  disabled: boolean;
}

export interface UserTeamMembership {
  teamIdentifier: string;
  teamName: string;
  role: string;
  isOwner: boolean;
  demo: boolean;
  joinedAt: string;
}

export interface BackofficeUserDetail extends BackofficeUser {
  teams: UserTeamMembership[];
}

export interface DataCounts {
  properties: number;
  contacts: number;
  contracts: number;
  expenses: number;
  payments: number;
  documents: number;
}

export interface FinancialSnapshot {
  totalActiveRent: number;
  currency?: string;
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
  phone?: string;
  emailVerified: boolean;
  disabled: boolean;
  online: boolean;
  teamCount: number;
  demoTeamCount: number;
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
  recipientEmail?: string;
  recipientPhone?: string;
  status: string;
  providerStatus?: string;
  providerError?: string;
  openCount: number;
  clickCount: number;
  firstOpenedAt?: string;
  firstClickedAt?: string;
  resentFromIdentifier?: string;
  resendReason?: string;
  createdAt: string;
  statusUpdatedAt?: string;
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
  triggerName?: string;
  triggerGroup?: string;
  triggerType?: 'cron' | 'simple';
  scheduleExpression?: string;
  triggerState: string;
  nextFireTime?: string;
  previousFireTime?: string;
}

export interface JobExecutionHistory {
  id: string;
  jobName: string;
  jobGroup: string;
  startedAt: string;
  endedAt?: string;
  durationMs?: number;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  errorMessage?: string;
  nodeId?: string;
}

export interface LoggerConfiguration {
  name: string;
  configuredLevel?: string;
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
  createdAt?: string;
  lastLogin?: string;
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

// Broadcast types
export type BroadcastSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type BroadcastScope = 'GLOBAL' | 'TEAMS' | 'USERS';

export interface BroadcastMessage {
  identifier: string;
  title: string;
  body: string;
  severity: BroadcastSeverity;
  scope: BroadcastScope;
  startAt: string;
  endAt?: string;
  showOnLogin: boolean;
  showOnRegister: boolean;
  showInApp: boolean;
  targetTeamIdentifiers: string[];
  targetUserIdentifiers: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateBroadcastMessageRequest {
  title: string;
  body: string;
  severity: BroadcastSeverity;
  scope: BroadcastScope;
  startAt: string;
  endAt?: string;
  showOnLogin: boolean;
  showOnRegister: boolean;
  showInApp: boolean;
  targetTeamIdentifiers?: string[];
  targetUserIdentifiers?: string[];
}

export interface UpdateBroadcastMessageRequest {
  title: string;
  body: string;
  severity: BroadcastSeverity;
  scope: BroadcastScope;
  startAt: string;
  endAt?: string;
  showOnLogin: boolean;
  showOnRegister: boolean;
  showInApp: boolean;
  targetTeamIdentifiers?: string[];
  targetUserIdentifiers?: string[];
}

// System Info types
export type ServiceHealthStatus = 'UP' | 'DOWN' | 'DISABLED' | 'UNKNOWN';

export interface ServiceHealth {
  name: string;
  status: ServiceHealthStatus;
  latencyMs?: number;
  details?: string;
  error?: string;
}

export interface MigrationEntry {
  version?: string;
  description: string;
  state: string;
  installedOn?: string;
  executionTimeMs?: number;
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
    gitCommit?: string;
    gitCommitFull?: string;
    gitBranch?: string;
    gitCommitTime?: string;
    gitDirty: boolean;
    buildTime?: string;
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
    currentVersion?: string;
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
