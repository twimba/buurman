import client from './client';
import {
  TenantResponse,
  CreateTenantRequest,
  UpdateTenantRequest,
  LinkTenantToPropertyRequest,
  PropertyTenantHistoryResponse,
  TenantAddressResponse,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
} from '../types/tenant';
import { DocumentResponse } from '../types/property';
import { RecentActivity } from './dashboard';

export const getTenants = async (
  search?: string
): Promise<TenantResponse[]> => {
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

export const getTenantDocuments = async (
  tenantId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/tenants/${tenantId}/documents`);
  return response.data;
};

export const getTenantPhotos = async (
  tenantId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/tenants/${tenantId}/photos`);
  return response.data;
};

export const uploadTenantDocument = async (
  tenantId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

  const response = await client.post(
    `/tenants/${tenantId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const uploadTenantPhoto = async (
  tenantId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

  const response = await client.post(`/tenants/${tenantId}/photos`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const setTenantMainPhoto = async (
  tenantId: string,
  photoId: string
): Promise<DocumentResponse> => {
  const response = await client.put(
    `/tenants/${tenantId}/photos/${photoId}/set-main`
  );
  return response.data;
};

export const deleteTenantDocument = async (
  documentId: string
): Promise<void> => {
  await client.delete(`/tenants/documents/${documentId}`);
};

export const getTenantAuditLog = async (
  tenantId: string
): Promise<RecentActivity[]> => {
  const response = await client.get(`/tenants/${tenantId}/audit-log`);
  return response.data;
};

export const getTenantAddresses = async (
  tenantId: string
): Promise<TenantAddressResponse[]> => {
  const response = await client.get(`/tenants/${tenantId}/addresses`);
  return response.data;
};

export const createTenantAddress = async (
  tenantId: string,
  data: CreateTenantAddressRequest
): Promise<TenantAddressResponse> => {
  const response = await client.post(`/tenants/${tenantId}/addresses`, data);
  return response.data;
};

export const updateTenantAddress = async (
  tenantId: string,
  addressId: string,
  data: UpdateTenantAddressRequest
): Promise<TenantAddressResponse> => {
  const response = await client.put(
    `/tenants/${tenantId}/addresses/${addressId}`,
    data
  );
  return response.data;
};

export const deleteTenantAddress = async (
  tenantId: string,
  addressId: string
): Promise<void> => {
  await client.delete(`/tenants/${tenantId}/addresses/${addressId}`);
};
