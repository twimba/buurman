import client from './client';
import {
  TenantResponse,
  CreateTenantRequest,
  UpdateTenantRequest,
  LinkTenantToPropertyRequest,
  PropertyTenantHistoryResponse,
} from '../types/tenant';

export const getTenants = async (search?: string): Promise<TenantResponse[]> => {
  const params = search ? { search } : {};
  const response = await client.get('/tenants', { params });
  return response.data;
};

export const getTenant = async (id: string): Promise<TenantResponse> => {
  const response = await client.get(`/tenants/${id}`);
  return response.data;
};

export const createTenant = async (
  data: CreateTenantRequest
): Promise<TenantResponse> => {
  const response = await client.post('/tenants', data);
  return response.data;
};

export const updateTenant = async (
  id: string,
  data: UpdateTenantRequest
): Promise<TenantResponse> => {
  const response = await client.put(`/tenants/${id}`, data);
  return response.data;
};

export const deleteTenant = async (id: string): Promise<void> => {
  await client.delete(`/tenants/${id}`);
};

export const linkTenantToProperty = async (
  tenantId: string,
  data: LinkTenantToPropertyRequest
): Promise<TenantResponse> => {
  const response = await client.post(
    `/tenants/${tenantId}/link-property`,
    data
  );
  return response.data;
};

export const unlinkTenantFromProperty = async (
  tenantId: string
): Promise<TenantResponse> => {
  const response = await client.post(`/tenants/${tenantId}/unlink-property`);
  return response.data;
};

export const getTenantHistory = async (
  tenantId: string
): Promise<PropertyTenantHistoryResponse[]> => {
  const response = await client.get(`/tenants/${tenantId}/history`);
  return response.data;
};
