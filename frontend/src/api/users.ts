import client from './client';

export interface UserProfileResponse {
  identifier: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
}

export interface UpdateUserProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
}

export interface UserTeamResponse {
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
  teamIdentifier: string;
}

export interface SetDefaultTeamRequest {
  teamIdentifier: string;
}

export interface UserPreferencesResponse {
  theme: string;
  language: string;
  timezone: string;
  dateFormat: string;
  currencyFormat: string;
  emailNotifications: boolean;
  inAppNotifications: boolean;
  smsNotifications: boolean;
}

export interface UpdateUserPreferencesRequest {
  theme?: string;
  language?: string;
  timezone?: string;
  dateFormat?: string;
  currencyFormat?: string;
  emailNotifications?: boolean;
  inAppNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface UserTeamNotificationPreferencesResponse {
  paymentReminders: boolean;
  contractExpiryAlerts: boolean;
  newMemberNotifications: boolean;
  weeklySummary: boolean;
  preferredChannels: string[];
}

export interface UpdateTeamNotificationPreferencesRequest {
  paymentReminders?: boolean;
  contractExpiryAlerts?: boolean;
  newMemberNotifications?: boolean;
  weeklySummary?: boolean;
  preferredChannels?: string[];
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

export const switchTeam = async (
  teamIdentifier: string
): Promise<UserTeamResponse> => {
  const response = await client.post('/users/switch-team', { teamIdentifier });
  return response.data;
};

export const setDefaultTeam = async (
  teamIdentifier: string
): Promise<UserTeamResponse> => {
  const response = await client.put('/users/default-team', { teamIdentifier });
  return response.data;
};

export const leaveTeam = async (teamIdentifier: string): Promise<void> => {
  await client.post(`/users/teams/${teamIdentifier}/leave`);
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
  teamIdentifier: string
): Promise<UserTeamNotificationPreferencesResponse> => {
  const response = await client.get(
    `/users/preferences/teams/${teamIdentifier}`
  );
  return response.data;
};

export const updateTeamNotificationPreferences = async (
  teamIdentifier: string,
  data: UpdateTeamNotificationPreferencesRequest
): Promise<UserTeamNotificationPreferencesResponse> => {
  const response = await client.patch(
    `/users/preferences/teams/${teamIdentifier}`,
    data
  );
  return response.data;
};
