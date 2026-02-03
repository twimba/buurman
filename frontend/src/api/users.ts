import client from './client';

export interface UserProfileResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UpdateUserProfileRequest {
  firstName: string;
  lastName: string;
}

export interface UserTeamResponse {
  teamId: string;
  teamName: string;
  identifier: string;
  role: 'TEAM_ADMIN' | 'TEAM_EDITOR' | 'TEAM_VIEWER';
  isOwner: boolean;
  isDefault: boolean;
  isActive: boolean;
  memberCount: number;
  joinedAt: string;
}

export interface SwitchTeamRequest {
  teamId: string;
}

export interface SetDefaultTeamRequest {
  teamId: string;
}

export interface UserPreferencesResponse {
  theme: string;
  language: string;
  timezone: string;
  dateFormat: string;
  currencyFormat: string;
  emailNotifications: boolean;
  inAppNotifications: boolean;
}

export interface UpdateUserPreferencesRequest {
  theme?: string;
  language?: string;
  timezone?: string;
  dateFormat?: string;
  currencyFormat?: string;
  emailNotifications?: boolean;
  inAppNotifications?: boolean;
}

export interface UserTeamNotificationPreferencesResponse {
  teamId: string;
  paymentReminders: boolean;
  contractExpiryAlerts: boolean;
  newMemberNotifications: boolean;
  weeklySummary: boolean;
}

export interface UpdateTeamNotificationPreferencesRequest {
  paymentReminders?: boolean;
  contractExpiryAlerts?: boolean;
  newMemberNotifications?: boolean;
  weeklySummary?: boolean;
}

// User profile
export const getCurrentUser = async (): Promise<UserProfileResponse> => {
  const response = await client.get('/users/me');
  return response.data;
};

export const updateUserProfile = async (
  data: UpdateUserProfileRequest
): Promise<UserProfileResponse> => {
  const response = await client.put('/users/me', data);
  return response.data;
};

// Team management
export const getUserTeams = async (): Promise<UserTeamResponse[]> => {
  const response = await client.get('/users/me/teams');
  return response.data;
};

export const switchTeam = async (teamId: string): Promise<UserTeamResponse> => {
  const response = await client.post('/users/switch-team', { teamId });
  return response.data;
};

export const setDefaultTeam = async (
  teamId: string
): Promise<UserTeamResponse> => {
  const response = await client.put('/users/default-team', { teamId });
  return response.data;
};

export const leaveTeam = async (teamId: string): Promise<void> => {
  await client.post(`/users/teams/${teamId}/leave`);
};

// User preferences
export const getUserPreferences =
  async (): Promise<UserPreferencesResponse> => {
    const response = await client.get('/users/preferences');
    return response.data;
  };

export const updateUserPreferences = async (
  data: UpdateUserPreferencesRequest
): Promise<UserPreferencesResponse> => {
  const response = await client.patch('/users/preferences', data);
  return response.data;
};

// Team notification preferences
export const getTeamNotificationPreferences = async (
  teamId: string
): Promise<UserTeamNotificationPreferencesResponse> => {
  const response = await client.get(`/users/preferences/teams/${teamId}`);
  return response.data;
};

export const updateTeamNotificationPreferences = async (
  teamId: string,
  data: UpdateTeamNotificationPreferencesRequest
): Promise<UserTeamNotificationPreferencesResponse> => {
  const response = await client.patch(
    `/users/preferences/teams/${teamId}`,
    data
  );
  return response.data;
};
