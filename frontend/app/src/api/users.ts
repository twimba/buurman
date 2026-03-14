import client from './client';

export interface UserProfileResponse {
  identifier: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  phoneVerified: boolean;
}

/** Absent fields are interpreted as "clear" (set to null) by the backend. */
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
  smsNotifications: boolean;
}

export interface UpdateUserPreferencesRequest {
  theme?: string;
  language?: string;
  timezone?: string;
  dateFormat?: string;
  currencyFormat?: string;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface NotificationTypePreferenceEntry {
  notificationType: string;
  displayName: string;
  emailEnabled: boolean;
  smsEnabled: boolean;
}

export interface NotificationTypePreferencesResponse {
  globalEmailEnabled: boolean;
  globalSmsEnabled: boolean;
  smsAvailable: boolean;
  emailAvailable: boolean;
  preferences: NotificationTypePreferenceEntry[];
}

export interface UpdateNotificationTypePreferencesRequest {
  preferences: {
    notificationType: string;
    emailEnabled: boolean;
    smsEnabled: boolean;
  }[];
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

// Phone verification
export const verifyPhone = async (
  code: string
): Promise<UserProfileResponse> => {
  const response = await client.post('/users/me/phone/verify', { code });
  return response.data;
};

export const resendPhoneVerification = async (): Promise<void> => {
  await client.post('/users/me/phone/resend-verification');
};

export const cancelPhoneVerification =
  async (): Promise<UserProfileResponse> => {
    const response = await client.post('/users/me/phone/cancel-verification');
    return response.data;
  };

// Phone number policy
export interface PhoneNumberPolicyResponse {
  policyMatrix: Record<string, string[]>;
}

export const getPhonePolicy = async (): Promise<PhoneNumberPolicyResponse> => {
  const response = await client.get('/users/phone-policy');
  return response.data;
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

// Notification type preferences
export const getNotificationTypePreferences =
  async (): Promise<NotificationTypePreferencesResponse> => {
    const response = await client.get('/users/preferences/notifications');
    return response.data;
  };

export const updateNotificationTypePreferences = async (
  data: UpdateNotificationTypePreferencesRequest
): Promise<NotificationTypePreferencesResponse> => {
  const response = await client.put('/users/preferences/notifications', data);
  return response.data;
};
