import client from './client';

export interface RegistrationInvitation {
  identifier: string;
  code: string;
  maxUsages?: number;
  usageCount: number;
  expiresAt?: string;
  revoked: boolean;
  status: 'ACTIVE' | 'EXPIRED' | 'EXHAUSTED' | 'REVOKED';
  createdBy: string;
  createdAt: string;
  hasNote: boolean;
}

export interface UsageRecord {
  userEmail: string;
  userName: string;
  usedAt: string;
}

export interface RegistrationInvitationDetail extends RegistrationInvitation {
  revokedBy?: string;
  revokedAt?: string;
  updatedAt: string;
  note?: string;
  usages: UsageRecord[];
}

export interface CreateRegistrationInvitationRequest {
  code?: string;
  maxUsages?: number;
  expiresAt?: string;
  note?: string;
}

/** Absent fields are interpreted as "clear" (set to null) by the backend. */
export interface UpdateRegistrationInvitationNoteRequest {
  note?: string;
}

export interface SendRegistrationInvitationRequest {
  recipient: string;
  channel: 'EMAIL' | 'SMS';
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
      '/registration-invitations',
      { params }
    ),

  get: (identifier: string) =>
    client.get<RegistrationInvitationDetail>(
      `/registration-invitations/${identifier}`
    ),

  create: (data: CreateRegistrationInvitationRequest) =>
    client.post<RegistrationInvitation>('/registration-invitations', data),

  revoke: (identifier: string) =>
    client.post(`/registration-invitations/${identifier}/revoke`),

  send: (identifier: string, data: SendRegistrationInvitationRequest) =>
    client.post(`/registration-invitations/${identifier}/send`, data),

  suggestCode: () =>
    client
      .get<{ code: string }>('/registration-invitations/suggest-code')
      .then((res) => res.data.code),

  updateNote: (
    identifier: string,
    data: UpdateRegistrationInvitationNoteRequest
  ) =>
    client.put<RegistrationInvitationDetail>(
      `/registration-invitations/${identifier}/note`,
      data
    ),
};
