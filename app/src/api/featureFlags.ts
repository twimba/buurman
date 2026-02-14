import client from './client';

export interface FlagData {
  enabled: boolean;
  value: unknown;
}

export type FeatureFlagsResponse = Record<string, FlagData>;

export const getFeatureFlags = async (): Promise<FeatureFlagsResponse> => {
  const { data } = await client.get<FeatureFlagsResponse>('/feature-flags');
  return data;
};
