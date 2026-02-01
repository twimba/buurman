import client from './client';
import {
  PropertyResponse,
  CreatePropertyRequest,
  UpdatePropertyRequest,
  PropertyStatus,
  DocumentResponse,
} from '../types/property';
import { RecentActivity } from './dashboard';

export const getProperties = async (
  status?: PropertyStatus
): Promise<PropertyResponse[]> => {
  const params = status ? { status } : {};
  const response = await client.get('/properties', { params });
  return response.data;
};

export const getProperty = async (id: string): Promise<PropertyResponse> => {
  const response = await client.get(`/properties/${id}`);
  return response.data;
};

export const createProperty = async (
  data: CreatePropertyRequest
): Promise<PropertyResponse> => {
  const response = await client.post('/properties', data);
  return response.data;
};

export const updateProperty = async (
  id: string,
  data: UpdatePropertyRequest
): Promise<PropertyResponse> => {
  const response = await client.put(`/properties/${id}`, data);
  return response.data;
};

export const deleteProperty = async (id: string): Promise<void> => {
  await client.delete(`/properties/${id}`);
};

export const getPropertyDocuments = async (
  propertyId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/properties/${propertyId}/documents`);
  return response.data;
};

export const uploadPropertyDocument = async (
  propertyId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

  const response = await client.post(
    `/properties/${propertyId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const getDocumentDownloadUrl = async (
  documentId: string
): Promise<{ url: string }> => {
  const response = await client.get(
    `/properties/documents/${documentId}/download`
  );
  return response.data;
};

export const deleteDocument = async (documentId: string): Promise<void> => {
  await client.delete(`/properties/documents/${documentId}`);
};

export const getPropertyAuditLog = async (
  propertyId: string
): Promise<RecentActivity[]> => {
  const response = await client.get(`/properties/${propertyId}/audit-log`);
  return response.data;
};
