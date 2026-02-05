import client from './client';
import { DocumentResponse } from '@/types/property';
import { PageResponse, PageParams } from '@/types/common';

export interface SearchDocumentsParams {
  search?: string;
  entityType?: string;
}

export const searchDocuments = async (
  params?: SearchDocumentsParams & PageParams
): Promise<PageResponse<DocumentResponse>> => {
  const response = await client.get('/documents', { params });
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
  documentIdentifiers: string[]
): Promise<Blob> => {
  const response = await client.post(
    '/documents/bulk-download',
    { documentIdentifiers },
    {
      responseType: 'blob',
    }
  );
  return response.data;
};

export const updateDocument = async (
  id: string,
  data: { title: string | null; notes: string | null }
): Promise<DocumentResponse> => {
  const response = await client.put(`/documents/${id}`, data);
  return response.data;
};
