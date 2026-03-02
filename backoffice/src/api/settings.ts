import client from "./client";

export interface CountryEntry {
  code: string;
  name: string;
}

export interface CountryGroupResponse {
  groupId: string;
  groupName: string;
  countries: CountryEntry[];
}

export interface PhonePolicyMetadataResponse {
  countryGroups: CountryGroupResponse[];
  numberTypes: string[];
}

export interface PhoneNumberPolicyResponse {
  policyMatrix: Record<string, string[]>;
  maxCodesPerHour: number;
  verificationCodeExpiryMinutes: number;
  updatedAt?: string;
  updatedBy?: string;
}

export interface UpdatePhoneNumberPolicyRequest {
  policyMatrix: Record<string, string[]>;
  maxCodesPerHour: number;
  verificationCodeExpiryMinutes: number;
}

export const getPhonePolicy = async (): Promise<PhoneNumberPolicyResponse> => {
  const response = await client.get("/settings/phone-policy");
  return response.data;
};

export const getPhonePolicyMetadata =
  async (): Promise<PhonePolicyMetadataResponse> => {
    const response = await client.get("/settings/phone-policy/metadata");
    return response.data;
  };

export const updatePhonePolicy = async (
  data: UpdatePhoneNumberPolicyRequest,
): Promise<PhoneNumberPolicyResponse> => {
  const response = await client.put("/settings/phone-policy", data);
  return response.data;
};

// ── Rate Limit Config ────────────────────────────────────────────────

export interface RateLimitConfigResponse {
  key: string;
  displayName: string;
  description?: string;
  maxRequests: number;
  periodSeconds: number;
  enabled: boolean;
  updatedAt?: string;
  updatedBy?: string;
}

export interface UpdateRateLimitConfigRequest {
  maxRequests: number;
  periodSeconds: number;
  enabled: boolean;
}

export const getRateLimitConfig = async (
  key: string,
): Promise<RateLimitConfigResponse> => {
  const response = await client.get(`/settings/rate-limits/${key}`);
  return response.data;
};

export const updateRateLimitConfig = async (
  key: string,
  data: UpdateRateLimitConfigRequest,
): Promise<RateLimitConfigResponse> => {
  const response = await client.put(`/settings/rate-limits/${key}`, data);
  return response.data;
};
