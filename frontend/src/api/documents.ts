import client from './client';
import { DocumentResponse } from '@/types/property';

export interface SearchDocumentsParams {
  search?: string;
  entityType?: string;
}

export const searchDocuments = async (
  params?: SearchDocumentsParams
): Promise<DocumentResponse[]> => {
  const queryParams = new URLSearchParams();
  if (params?.search) queryParams.append('search', params.search);
  if (params?.entityType) queryParams.append('entityType', params.entityType);

  const response = await client.get(`/documents?${queryParams.toString()}`);
  return response.data;
};

export const getDocument = async (id: string): Promise<DocumentResponse> => {
  const response = await client.get(`/documents/${id}`);
  return response.data;
};

export const getDownloadUrl = async (id: string): Promise<string> => {
  const response = await client.get(`/documents/${id}/download`);
  return response.data;
};

export const getPreviewUrl = async (id: string): Promise<string> => {
  const response = await client.get(`/documents/${id}/preview`);
  return response.data;
};

export const deleteDocument = async (id: string): Promise<void> => {
  await client.delete(`/documents/${id}`);
};

export const bulkDownloadDocuments = async (
  documentIds: string[]
): Promise<Blob> => {
  const response = await client.post('/documents/bulk-download', documentIds, {
    responseType: 'blob',
  });
  return response.data;
};
