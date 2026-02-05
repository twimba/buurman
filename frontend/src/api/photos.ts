import client from './client';
import { PhotoResponse } from '@/types/property';
import { PageResponse, PageParams } from '@/types/common';

export interface SearchPhotosParams {
  search?: string;
  entityType?: string;
}

export const searchPhotos = async (
  params?: SearchPhotosParams & PageParams
): Promise<PageResponse<PhotoResponse>> => {
  const response = await client.get('/photos', { params });
  return response.data;
};

export const getPhoto = async (id: string): Promise<PhotoResponse> => {
  const response = await client.get(`/photos/${id}`);
  return response.data;
};

export const getDownloadUrl = async (id: string): Promise<string> => {
  const response = await client.get(`/photos/${id}/download`);
  return response.data;
};

export const deletePhoto = async (id: string): Promise<void> => {
  await client.delete(`/photos/${id}`);
};

export const bulkDownloadPhotos = async (
  photoIdentifiers: string[]
): Promise<Blob> => {
  const response = await client.post(
    '/photos/bulk-download',
    { documentIdentifiers: photoIdentifiers },
    {
      responseType: 'blob',
    }
  );
  return response.data;
};

export const updatePhoto = async (
  id: string,
  data: { title: string | null; notes: string | null }
): Promise<PhotoResponse> => {
  const response = await client.put(`/photos/${id}`, data);
  return response.data;
};
