import client from "./client";

export interface RegistrationInvitation {
  identifier: string;
  code: string;
  maxUsages: number | null;
  usageCount: number;
  expiresAt: string | null;
  revoked: boolean;
  status: "ACTIVE" | "EXPIRED" | "EXHAUSTED" | "REVOKED";
  createdBy: string;
  createdAt: string;
}

export interface UsageRecord {
  userEmail: string;
  userName: string;
  usedAt: string;
}

export interface RegistrationInvitationDetail extends RegistrationInvitation {
  revokedBy: string | null;
  revokedAt: string | null;
  updatedAt: string;
  usages: UsageRecord[];
}

export interface CreateRegistrationInvitationRequest {
  code?: string;
  maxUsages?: number | null;
  expiresAt?: string | null;
}

export interface SendRegistrationInvitationRequest {
  recipient: string;
  channel: "EMAIL" | "SMS";
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const registrationInvitationsApi = {
  list: (params?: Record<string, unknown>) =>
    client.get<PageResponse<RegistrationInvitation>>(
      "/registration-invitations",
      { params },
    ),

  get: (identifier: string) =>
    client.get<RegistrationInvitationDetail>(
      `/registration-invitations/${identifier}`,
    ),

  create: (data: CreateRegistrationInvitationRequest) =>
    client.post<RegistrationInvitation>("/registration-invitations", data),

  revoke: (identifier: string) =>
    client.post(`/registration-invitations/${identifier}/revoke`),

  send: (identifier: string, data: SendRegistrationInvitationRequest) =>
    client.post(`/registration-invitations/${identifier}/send`, data),

  suggestCode: () =>
    client
      .get<{ code: string }>("/registration-invitations/suggest-code")
      .then((res) => res.data.code),
};
