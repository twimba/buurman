import client from './client';

export interface TeamResponse {
  identifier: string;
  teamName: string;
  memberCount: number;
  createdAt: string;
}

export interface TeamMemberResponse {
  userIdentifier: string;
  email: string;
  name: string;
  role: string;
  isOwner: boolean;
  joinedAt: string;
  isCurrentUser: boolean;
}

export interface TransferOwnershipRequest {
  newOwnerIdentifier: string;
}

export interface UpdateTeamRequest {
  name: string;
}

export interface TeamSettingsResponse {
  payments: {
    paymentsAheadCount: number;
    autoGenerationEnabled: boolean;
  };
  regional: {
    defaultCurrency: string;
    defaultCountryCode: string;
    timezone: string;
    dateFormat: string;
    fiscalYearStartMonth: string;
    defaultLanguage: string;
  };
}

export interface UpdateTeamSettingsRequest {
  payments?: {
    paymentsAheadCount: number;
    autoGenerationEnabled: boolean;
  };
  regional?: {
    defaultCurrency?: string;
    defaultCountryCode?: string;
    timezone?: string;
    dateFormat?: string;
    fiscalYearStartMonth?: string;
    defaultLanguage?: string;
  };
}

export interface CreateInvitationRequest {
  email: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
}

export interface InvitationResponse {
  token: string;
  email: string;
  teamIdentifier: string;
  teamName: string;
  role: string;
  inviterName: string;
  invitedAt: string;
  expiresAt: string;
  invitationUrl: string;
  isExpired: boolean;
  isAccepted: boolean;
}

export interface UpdateMemberRoleRequest {
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
}

export const getCurrentTeam = async (): Promise<TeamResponse> => {
  const response = await client.get('/teams/current');
  return response.data;
};

export const getTeamMembers = async (
  teamId: string
): Promise<TeamMemberResponse[]> => {
  const response = await client.get(`/teams/${teamId}/members`);
  return response.data;
};

export const createInvitation = async (
  teamId: string,
  data: CreateInvitationRequest
): Promise<InvitationResponse> => {
  const response = await client.post(`/teams/${teamId}/invitations`, data);
  return response.data;
};

export const removeMember = async (
  teamId: string,
  memberId: string
): Promise<void> => {
  await client.delete(`/teams/${teamId}/members/${memberId}`);
};

export const updateMemberRole = async (
  teamId: string,
  memberId: string,
  data: UpdateMemberRoleRequest
): Promise<TeamMemberResponse> => {
  const response = await client.put(
    `/teams/${teamId}/members/${memberId}/role`,
    data
  );
  return response.data;
};

export const getInvitation = async (
  token: string
): Promise<InvitationResponse> => {
  const response = await client.get(`/invitations/${token}`);
  return response.data;
};

export const acceptInvitation = async (token: string): Promise<void> => {
  await client.post(`/invitations/${token}/accept`);
};

export const getPendingInvitations = async (): Promise<
  InvitationResponse[]
> => {
  const response = await client.get('/invitations/pending');
  return response.data;
};

export const getTeamPendingInvitations = async (
  teamId: string
): Promise<InvitationResponse[]> => {
  const response = await client.get(`/teams/${teamId}/invitations/pending`);
  return response.data;
};

export const resendInvitation = async (
  teamId: string,
  token: string
): Promise<InvitationResponse> => {
  const response = await client.post(
    `/teams/${teamId}/invitations/${token}/resend`
  );
  return response.data;
};

export const transferOwnership = async (
  teamId: string,
  newOwnerIdentifier: string
): Promise<TeamMemberResponse> => {
  const response = await client.post(`/teams/${teamId}/transfer-ownership`, {
    newOwnerIdentifier,
  });
  return response.data;
};

export const updateTeam = async (
  teamId: string,
  data: UpdateTeamRequest
): Promise<TeamResponse> => {
  const response = await client.put(`/teams/${teamId}`, data);
  return response.data;
};

export const getTeamSettings = async (
  teamId: string
): Promise<TeamSettingsResponse> => {
  const response = await client.get(`/teams/${teamId}/settings`);
  return response.data;
};

export const updateTeamSettings = async (
  teamId: string,
  data: UpdateTeamSettingsRequest
): Promise<TeamSettingsResponse> => {
  const response = await client.put(`/teams/${teamId}/settings`, data);
  return response.data;
};
