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

export const featureFlagsApi = {
  getGlobal: () => client.get<FlagMap>("/feature-flags"),
  getForUser: (userIdentifier: string) =>
    client.get<TeamFlagEvaluation[]>(`/feature-flags/users/${userIdentifier}`),
};
