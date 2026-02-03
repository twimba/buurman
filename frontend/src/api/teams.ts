import client from './client';

export interface TeamResponse {
  teamId: string;
  identifier: string;
  teamName: string;
  memberCount: number;
  createdAt: string;
}

export interface TeamMemberResponse {
  memberId: string;
  userId: string;
  email: string;
  name: string;
  role: string;
  isOwner: boolean;
  joinedAt: string;
  isCurrentUser: boolean;
}

export interface TransferOwnershipRequest {
  newOwnerId: string;
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
    defaultCountry: string;
    timezone: string;
    dateFormat: string;
    fiscalYearStartMonth: string;
  };
}

export interface UpdateTeamSettingsRequest {
  payments?: {
    paymentsAheadCount: number;
    autoGenerationEnabled: boolean;
  };
  regional?: {
    defaultCurrency?: string;
    defaultCountry?: string;
    timezone?: string;
    dateFormat?: string;
    fiscalYearStartMonth?: string;
  };
}

export interface CreateInvitationRequest {
  email: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
}

export interface InvitationResponse {
  invitationId: string;
  token: string;
  email: string;
  teamId: string;
  teamName: string;
  role: string;
  inviterName: string;
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

export const transferOwnership = async (
  teamId: string,
  newOwnerId: string
): Promise<TeamMemberResponse> => {
  const response = await client.post(`/teams/${teamId}/transfer-ownership`, {
    newOwnerId,
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
): Promise<TeamResponse> => {
  const response = await client.put(`/teams/${teamId}/settings`, data);
  return response.data;
};
