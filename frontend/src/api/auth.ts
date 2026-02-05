import client from './client';

export interface RegisterRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface UserResponse {
  identifier: string;
  email: string;
  firstName: string;
  lastName: string;
  teamIdentifier: string;
  teamName: string;
  role: string;
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
