// Re-exported generated models (spec is source of truth). Names preserved for callers.
export type {
  BackofficeTeamResponse as BackofficeTeam,
  BackofficeTeamDetailResponse as BackofficeTeamDetail,
  MemberInfo as TeamMemberInfo,
  UserTeamMembership,
  BackofficeUserResponse as BackofficeUser,
  BackofficeUserDetailResponse as BackofficeUserDetail,
  DataCounts,
  FinancialSnapshot,
  SettingsInfo as TeamSettingsInfo,
  BuurmyResponse as Buurmy,
  CreateBuurmyRequest,
} from '../generated/models';

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

export type { NotificationStatsResponse as NotificationStats } from '../generated/models';

export type {
  ScheduledJobResponse as ScheduledJob,
  JobExecutionHistoryResponse as JobExecutionHistory,
} from '../generated/models';

export type {
  RateLimitBucketResponse as RateLimitBucketEntry,
  RateLimitBucketResponse as RateLimitBucketDetail,
  RateLimitBucketPageResponse as RateLimitBucketListResponse,
  RateLimitConfigSummary,
  RateLimitBucketSummaryResponse as RateLimitSummaryResponse,
} from '../generated/models';

export type { LoggerConfigurationResponse as LoggerConfiguration } from '../generated/models';

// Broadcast types — structure from the generated response; severity/scope narrowed to the
// UI unions (the backend models them as plain strings).
import type { BackofficeBroadcastMessageResponse } from '../generated/models';

export type BroadcastSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type BroadcastScope = 'GLOBAL' | 'TEAMS' | 'USERS';

export type BroadcastMessage = Omit<
  BackofficeBroadcastMessageResponse,
  'severity' | 'scope'
> & {
  severity: BroadcastSeverity;
  scope: BroadcastScope;
};

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

// ── Feature flags ────────────────────────────────────────────────────
import type { FeatureFlagState } from '../generated/models';

export type { FeatureFlagState as FlagStatus } from '../generated/models';

export type FlagMap = Record<string, FeatureFlagState>;

export type { TeamFlagEvaluation } from '../generated/models';

/** Absent fields are interpreted as "clear" (set to null) by the backend. */
export interface UpdateFlagRequest {
  enabled?: boolean;
  value?: string;
}

export interface FeatureFlagUpdateResponse {
  flagName: string;
  enabled: boolean;
  value: unknown;
}

export type {
  SegmentFlagOverride,
  SegmentEvaluation,
} from '../generated/models';

export type { FeatureFlagAdminStatusResponse as AdminStatus } from '../generated/models';

// ── Rent regulations ─────────────────────────────────────────────────
// Response types re-exported from generated models; request/form types kept
// here as form-only shapes.
export type {
  RentRegulationCountryResponse,
  RentRegulationRegionResponse,
  RentRegulationRuleResponse,
  RentRegulationCatalogInfo,
  RentRegulationReloadResult,
  RentRegulationCatalogDiff,
  RentRegulationCountryDiff,
  RentRegulationDiffEntry,
  RentRegulationDiffCounts,
  RentRegulationDiffField,
  CountryRegulationRequestSummary,
  CountryRegulationRequester,
} from '../generated/models';

export interface CreateCountryRequest {
  countryCode: string;
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
}

export interface UpdateCountryRequest {
  countryName: string;
  hasRegionalRegulations: boolean;
  summary?: string;
}

export interface CreateRegionRequest {
  regionCode: string;
  regionName: string;
  summary?: string;
}

export interface UpdateRegionRequest {
  regionName: string;
  summary?: string;
}

// Derived from the generated request so all rule fields stay reachable and any spec drift is
// caught. The form keeps `maxIncreaseType`/`frequency` as plain strings (dropdown values); the
// hooks narrow them to the generated enums when sending.
import type { CreateRentRegulationRuleRequest } from '../generated/models';

export type CreateRuleRequest = Omit<
  CreateRentRegulationRuleRequest,
  'maxIncreaseType' | 'frequency'
> & {
  maxIncreaseType: string;
  frequency: string;
};

export type UpdateRuleRequest = CreateRuleRequest;

export interface BulkRuleRequest {
  rules: CreateRuleRequest[];
}

// System Info types — re-exported from generated models (spec is source of truth)
export type {
  BackofficeSystemInfoResponse as SystemInfoResponse,
  ServiceHealth,
  ServiceHealthStatus,
  MigrationEntry,
  GcInfo,
  ConfigEntry,
  MetricEntry,
  HttpLatencyStats,
  MetricsSnapshot,
  SessionInfo,
} from '../generated/models';

// ── Phone policy metadata ────────────────────────────────────────────
export type {
  CountryEntry,
  CountryGroupResponse,
  PhonePolicyMetadataResponse,
} from '../generated/models';
