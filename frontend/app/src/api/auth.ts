import client from './client';

export interface RegisterRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  invitationToken?: string;
  registrationInvitationCode?: string;
}

export interface RegistrationConfig {
  invitationRequired: boolean;
}

export interface ValidateInvitationCodeResponse {
  valid: boolean;
}

export interface UserResponse {
  identifier: string;
  email: string;
  firstName: string;
  lastName: string;
  teamIdentifier: string;
  teamName: string;
  role: string;
  emailVerified: boolean;
  createdAt: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
}

export const register = async (
  data: RegisterRequest
): Promise<UserResponse> => {
  const response = await client.post('/auth/register', data);
  return response.data;
};

export const getCurrentUser = async (): Promise<UserResponse> => {
  const response = await client.get('/auth/me');
  return response.data;
};

export const updateProfile = async (
  data: UpdateProfileRequest
): Promise<UserResponse> => {
  const response = await client.put('/auth/me', data);
  return response.data;
};

export const verifyEmail = async (code: string): Promise<UserResponse> => {
  const response = await client.post('/auth/verify-email', { code });
  return response.data;
};

export const verifyEmailByToken = async (token: string): Promise<void> => {
  await client.get('/auth/verify-email-token', { params: { token } });
};

export const resendVerificationCode = async (): Promise<void> => {
  await client.post('/auth/resend-verification');
};

export const getRegistrationConfig = async (): Promise<RegistrationConfig> => {
  const response = await client.get('/registration/config');
  return response.data;
};

export const validateInvitationCode = async (
  code: string
): Promise<ValidateInvitationCodeResponse> => {
  const response = await client.post('/registration-invitations/validate', {
    code,
  });
  return response.data;
};
