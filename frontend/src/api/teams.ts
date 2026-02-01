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
  joinedAt: string;
  isCurrentUser: boolean;
}

export interface CreateInvitationRequest {
  email: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
}

export interface InvitationResponse {
  invitationId: string;
  token: string;
  email: string;
  teamName: string;
  role: string;
  expiresAt: string;
  invitationUrl: string;
}

export interface UpdateMemberRoleRequest {
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
}

export const getCurrentTeam = async (): Promise<TeamResponse> => {
  const response = await client.get('/teams/current');
  return response.data;
};

export const getTeamMembers = async (teamId: string): Promise<TeamMemberResponse[]> => {
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

export const removeMember = async (teamId: string, memberId: string): Promise<void> => {
  await client.delete(`/teams/${teamId}/members/${memberId}`);
};

export const updateMemberRole = async (
  teamId: string,
  memberId: string,
  data: UpdateMemberRoleRequest
): Promise<TeamMemberResponse> => {
  const response = await client.put(`/teams/${teamId}/members/${memberId}/role`, data);
  return response.data;
};

export const getInvitation = async (token: string): Promise<InvitationResponse> => {
  const response = await client.get(`/invitations/${token}`);
  return response.data;
};

export const acceptInvitation = async (token: string): Promise<void> => {
  await client.post(`/invitations/${token}/accept`);
};
