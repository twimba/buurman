import client from './client';
import {
  ContactResponse,
  CreateContactRequest,
  UpdateContactRequest,
  LinkContactToPropertyRequest,
  PropertyContactHistoryResponse,
  ContactAddressResponse,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
} from '../types/contact';
import { DocumentResponse, PhotoResponse } from '../types/property';
import { RecentActivity } from './dashboard';
import { PageResponse, PageParams } from '@/types/common';

export const getContacts = async (
  params?: { search?: string } & PageParams
): Promise<PageResponse<ContactResponse>> => {
  const response = await client.get('/contacts', { params });
  return response.data;
};

export const getContact = async (id: string): Promise<ContactResponse> => {
  const response = await client.get(`/contacts/${id}`);
  return response.data;
};

export const createContact = async (
  data: CreateContactRequest
): Promise<ContactResponse> => {
  const response = await client.post('/contacts', data);
  return response.data;
};

export const updateContact = async (
  id: string,
  data: UpdateContactRequest
): Promise<ContactResponse> => {
  const response = await client.put(`/contacts/${id}`, data);
  return response.data;
};

export const deleteContact = async (id: string): Promise<void> => {
  await client.delete(`/contacts/${id}`);
};

export const linkContactToProperty = async (
  contactId: string,
  data: LinkContactToPropertyRequest
): Promise<ContactResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/link-property`,
    data
  );
  return response.data;
};

export const unlinkContactFromProperty = async (
  contactId: string
): Promise<ContactResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/unlink-property`
  );
  return response.data;
};

export const getContactHistory = async (
  contactId: string
): Promise<PropertyContactHistoryResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/history`);
  return response.data;
};

export const getContactDocuments = async (
  contactId: string
): Promise<DocumentResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/documents`);
  return response.data;
};

export const getContactPhotos = async (
  contactId: string
): Promise<PhotoResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/photos`);
  return response.data;
};

export const uploadContactDocument = async (
  contactId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<DocumentResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) {
    formData.append('title', title);
  }
  if (notes) {
    formData.append('notes', notes);
  }

  const response = await client.post(
    `/contacts/${contactId}/documents`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const uploadContactPhoto = async (
  contactId: string,
  file: File,
  title?: string,
  notes?: string
): Promise<PhotoResponse> => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) {
    formData.append('title', title);
  }
  if (notes) {
    formData.append('notes', notes);
  }

  const response = await client.post(
    `/contacts/${contactId}/photos`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
};

export const setContactMainPhoto = async (
  contactId: string,
  photoId: string
): Promise<PhotoResponse> => {
  const response = await client.put(
    `/contacts/${contactId}/photos/${photoId}/set-main`
  );
  return response.data;
};

export const deleteContactDocument = async (
  documentId: string
): Promise<void> => {
  await client.delete(`/contacts/documents/${documentId}`);
};

export const getContactAuditLog = async (
  contactId: string
): Promise<RecentActivity[]> => {
  const response = await client.get(`/contacts/${contactId}/audit-log`);
  return response.data;
};

export const getContactAddresses = async (
  contactId: string
): Promise<ContactAddressResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/addresses`);
  return response.data;
};

export const createContactAddress = async (
  contactId: string,
  data: CreateContactAddressRequest
): Promise<ContactAddressResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/addresses`,
    data
  );
  return response.data;
};

export const updateContactAddress = async (
  contactId: string,
  addressId: string,
  data: UpdateContactAddressRequest
): Promise<ContactAddressResponse> => {
  const response = await client.put(
    `/contacts/${contactId}/addresses/${addressId}`,
    data
  );
  return response.data;
};

export const deleteContactAddress = async (
  contactId: string,
  addressId: string
): Promise<void> => {
  await client.delete(`/contacts/${contactId}/addresses/${addressId}`);
};
