import client from './client';
import {
  PropertyResponse,
  CreatePropertyRequest,
  UpdatePropertyRequest,
  PropertyStatus,
  DocumentResponse,
  PhotoResponse,
  OutdoorAreaResponse,
  OutdoorAreaRequest,
  AmenityResponse,
  PropertyAmenityResponse,
} from '../types/property';
import { RecentActivity } from './dashboard';
import { PageResponse, PageParams } from '@/types/common';

export const getProperties = async (
  params?: { status?: PropertyStatus } & PageParams
): Promise<PageResponse<PropertyResponse>> => {
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

export const getPropertyPhotos = async (
  propertyId: string
): Promise<PhotoResponse[]> => {
  const response = await client.get(`/properties/${propertyId}/photos`);
  return response.data;
};

export const uploadPropertyPhoto = async (
  propertyId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<PhotoResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (notes) formData.append('notes', notes);

  const response = await client.post(
    `/properties/${propertyId}/photos`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const setMainPhoto = async (
  propertyId: string,
  photoId: string
): Promise<PhotoResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/photos/${photoId}/set-main`
  );
  return response.data;
};

// --- Outdoor Areas ---

export const getOutdoorAreas = async (
  propertyId: string
): Promise<OutdoorAreaResponse[]> => {
  const response = await client.get(`/properties/${propertyId}/outdoor-areas`);
  return response.data;
};

export const createOutdoorArea = async (
  propertyId: string,
  data: OutdoorAreaRequest
): Promise<OutdoorAreaResponse> => {
  const response = await client.post(
    `/properties/${propertyId}/outdoor-areas`,
    data
  );
  return response.data;
};

export const updateOutdoorArea = async (
  propertyId: string,
  areaId: string,
  data: OutdoorAreaRequest
): Promise<OutdoorAreaResponse> => {
  const response = await client.put(
    `/properties/${propertyId}/outdoor-areas/${areaId}`,
    data
  );
  return response.data;
};

export const deleteOutdoorArea = async (
  propertyId: string,
  areaId: string
): Promise<void> => {
  await client.delete(`/properties/${propertyId}/outdoor-areas/${areaId}`);
};

// --- Amenities ---

export const getAmenities = async (): Promise<
  Record<string, AmenityResponse[]>
> => {
  const response = await client.get('/amenities');
  return response.data;
};

export const getPropertyAmenities = async (
  propertyId: string
): Promise<PropertyAmenityResponse[]> => {
  const response = await client.get(`/properties/${propertyId}/amenities`);
  return response.data;
};

export const addPropertyAmenity = async (
  propertyId: string,
  amenityIdentifier: string,
  notes?: string | null
): Promise<PropertyAmenityResponse> => {
  const response = await client.post(`/properties/${propertyId}/amenities`, {
    amenityIdentifier,
    notes,
  });
  return response.data;
};

export const removePropertyAmenity = async (
  propertyId: string,
  amenityIdentifier: string
): Promise<void> => {
  await client.delete(
    `/properties/${propertyId}/amenities/${amenityIdentifier}`
  );
};
