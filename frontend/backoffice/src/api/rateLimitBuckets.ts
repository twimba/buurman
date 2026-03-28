import client from "./client";

export interface RateLimitBucketEntry {
  bucketId: string;
  configKey: string;
  clientIdentifier: string;
  expiresAt: string;
  availableTokens: number | null;
  maxTokens: number | null;
  refillPeriodSeconds: number | null;
}

export interface RateLimitBucketDetail extends RateLimitBucketEntry {
  configEnabled: boolean;
  configDescription: string | null;
}

export interface RateLimitBucketListResponse {
  content: RateLimitBucketEntry[];
  page: number;
  size: number;
  totalElements: number;
}

export interface RateLimitConfigSummary {
  configKey: string;
  displayName: string;
  enabled: boolean;
  maxRequests: number;
  periodSeconds: number;
  activeBuckets: number;
}

export interface RateLimitSummaryResponse {
  configs: RateLimitConfigSummary[];
  totalActiveBuckets: number;
}

export const rateLimitBucketsApi = {
  getSummary: () =>
    client.get<RateLimitSummaryResponse>("/rate-limits/buckets/summary"),

  list: (params: {
    configKey?: string;
    clientIp?: string;
    page?: number;
    size?: number;
  }) =>
    client.get<RateLimitBucketListResponse>("/rate-limits/buckets", { params }),

  get: (bucketId: string) =>
    client.get<RateLimitBucketDetail>(
      `/rate-limits/buckets/${encodeURIComponent(bucketId)}`,
    ),

  deleteBucket: (bucketId: string) =>
    client.delete(`/rate-limits/buckets/${encodeURIComponent(bucketId)}`),

  deleteByConfigKey: (configKey: string) =>
    client.delete<{ deletedCount: number }>("/rate-limits/buckets", {
      params: { configKey },
    }),
};
