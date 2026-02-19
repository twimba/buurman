import client from "./client";

export interface FlagStatus {
  enabled: boolean;
  value: unknown;
}

export type FlagMap = Record<string, FlagStatus>;

export interface TeamFlagEvaluation {
  teamIdentifier: string;
  teamName: string;
  role: string;
  isOwner: boolean;
  isActive: boolean;
  flags: FlagMap;
}

export interface UpdateFlagRequest {
  enabled?: boolean;
  value?: string | null;
}

export interface FeatureFlagUpdateResponse {
  flagName: string;
  enabled: boolean;
  value: unknown;
}

export interface SegmentFlagOverride {
  flagName: string;
  enabled: boolean;
  value: unknown;
}

export interface SegmentEvaluation {
  segmentId: number;
  segmentName: string;
  description: string | null;
  overrides: Record<string, SegmentFlagOverride>;
}

export interface AdminStatus {
  adminConfigured: boolean;
  authMethod: "api_token" | "credentials" | "none";
}

export const featureFlagsApi = {
  getAdminStatus: () =>
    client.get<AdminStatus>("/feature-flags/admin-status"),
  getGlobal: () => client.get<FlagMap>("/feature-flags"),
  getForUser: (userIdentifier: string) =>
    client.get<TeamFlagEvaluation[]>(`/feature-flags/users/${userIdentifier}`),

  updateGlobalFlag: (flagName: string, data: UpdateFlagRequest) =>
    client.patch<FeatureFlagUpdateResponse>(`/feature-flags/${flagName}`, data),

  upsertIdentityOverride: (
    userIdentifier: string,
    teamIdentifier: string,
    flagName: string,
    data: UpdateFlagRequest,
  ) =>
    client.put<FeatureFlagUpdateResponse>(
      `/feature-flags/identities/${userIdentifier}/teams/${teamIdentifier}/${flagName}`,
      data,
    ),

  deleteIdentityOverride: (
    userIdentifier: string,
    teamIdentifier: string,
    flagName: string,
  ) =>
    client.delete(
      `/feature-flags/identities/${userIdentifier}/teams/${teamIdentifier}/${flagName}`,
    ),

  getSegments: () => client.get<SegmentEvaluation[]>("/feature-flags/segments"),

  upsertSegmentOverride: (
    segmentId: number,
    flagName: string,
    data: UpdateFlagRequest,
  ) =>
    client.put<FeatureFlagUpdateResponse>(
      `/feature-flags/segments/${segmentId}/${flagName}`,
      data,
    ),

  deleteSegmentOverride: (segmentId: number, flagName: string) =>
    client.delete(`/feature-flags/segments/${segmentId}/${flagName}`),
};
