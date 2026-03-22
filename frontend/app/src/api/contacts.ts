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
  ContactNoteResponse,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
  ContactRelationshipResponse,
  CreateContactRelationshipRequest,
  UpdateContactRelationshipRequest,
  AddContactTagRequest,
  ContactActivityItem,
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

// --- Contact Notes ---

export const getContactNotes = async (
  contactId: string
): Promise<ContactNoteResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/notes`);
  return response.data;
};

export const createContactNote = async (
  contactId: string,
  data: CreateContactNoteRequest
): Promise<ContactNoteResponse> => {
  const response = await client.post(`/contacts/${contactId}/notes`, data);
  return response.data;
};

export const updateContactNote = async (
  contactId: string,
  noteId: string,
  data: UpdateContactNoteRequest
): Promise<ContactNoteResponse> => {
  const response = await client.put(
    `/contacts/${contactId}/notes/${noteId}`,
    data
  );
  return response.data;
};

export const deleteContactNote = async (
  contactId: string,
  noteId: string
): Promise<void> => {
  await client.delete(`/contacts/${contactId}/notes/${noteId}`);
};

export const pinContactNote = async (
  contactId: string,
  noteId: string
): Promise<ContactNoteResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/notes/${noteId}/pin`
  );
  return response.data;
};

export const unpinContactNote = async (
  contactId: string,
  noteId: string
): Promise<ContactNoteResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/notes/${noteId}/unpin`
  );
  return response.data;
};

// --- Contact Relationships ---

export const getContactRelationships = async (
  contactId: string
): Promise<ContactRelationshipResponse[]> => {
  const response = await client.get(`/contacts/${contactId}/relationships`);
  return response.data;
};

export const createContactRelationship = async (
  contactId: string,
  data: CreateContactRelationshipRequest
): Promise<ContactRelationshipResponse> => {
  const response = await client.post(
    `/contacts/${contactId}/relationships`,
    data
  );
  return response.data;
};

export const updateContactRelationship = async (
  contactId: string,
  relationshipId: string,
  data: UpdateContactRelationshipRequest
): Promise<ContactRelationshipResponse> => {
  const response = await client.put(
    `/contacts/${contactId}/relationships/${relationshipId}`,
    data
  );
  return response.data;
};

export const deleteContactRelationship = async (
  contactId: string,
  relationshipId: string
): Promise<void> => {
  await client.delete(
    `/contacts/${contactId}/relationships/${relationshipId}`
  );
};

// --- Contact Tags ---

export const addContactTag = async (
  contactId: string,
  data: AddContactTagRequest
): Promise<ContactResponse> => {
  const response = await client.post(`/contacts/${contactId}/tags`, data);
  return response.data;
};

export const removeContactTag = async (
  contactId: string,
  tag: string
): Promise<ContactResponse> => {
  const response = await client.delete(`/contacts/${contactId}/tags/${tag}`);
  return response.data;
};

// --- Contact Activity ---

export const getContactActivity = async (
  contactId: string,
  params?: PageParams
): Promise<PageResponse<ContactActivityItem>> => {
  const response = await client.get(`/contacts/${contactId}/activity`, {
    params,
  });
  return response.data;
};

// --- Duplicate Check ---

export const checkContactDuplicates = async (
  data: CreateContactRequest
): Promise<{ matches: Array<{ contact: { identifier: string; firstName: string; lastName?: string; email?: string; phone?: string }; matchField: string; matchType: string }> }> => {
  const response = await client.post('/contacts/check-duplicates', data);
  return response.data;
};
