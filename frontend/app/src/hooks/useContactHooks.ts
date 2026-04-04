import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as contactsApi from '../api/contacts';
import {
  CreateContactRequest,
  UpdateContactRequest,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
  CreateContactRelationshipRequest,
  UpdateContactRelationshipRequest,
  AddContactTagRequest,
  ContactType,
  ContactTag,
  DuplicateCheckRequest,
  DuplicateCheckResponse,
} from '../types/contact';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useContacts = (
  params?: {
    search?: string;
    contactType?: ContactType;
    tags?: ContactTag[];
  } & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.contacts.all(params),
    queryFn: () => contactsApi.getContacts(params),
    placeholderData: keepPreviousData,
  });
};

export const useContact = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.detail(id),
    queryFn: () => contactsApi.getContact(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateContact = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContactRequest) => contactsApi.createContact(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contact created successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_CREATED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContact = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateContactRequest) =>
      contactsApi.updateContact(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contact updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContact = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => contactsApi.deleteContact(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Contact deleted successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_DELETED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContactHistory = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.history(contactId),
    queryFn: () => contactsApi.getContactHistory(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactAuditLog = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.auditLog(contactId),
    queryFn: () => contactsApi.getContactAuditLog(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactDocuments = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.documents(contactId),
    queryFn: () => contactsApi.getContactDocuments(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactPhotos = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.photos(contactId),
    queryFn: () => contactsApi.getContactPhotos(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useUploadContactDocument = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => contactsApi.uploadContactDocument(contactId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.documents(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.documents.all(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUploadContactPhoto = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => contactsApi.uploadContactPhoto(contactId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.photos(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useSetContactMainPhoto = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (photoId: string) =>
      contactsApi.setContactMainPhoto(contactId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.photos(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContactDocument = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      contactsApi.deleteContactDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.documents(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.documents.all(),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContactAddresses = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.addresses(contactId),
    queryFn: () => contactsApi.getContactAddresses(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactAddress = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContactAddressRequest) =>
      contactsApi.createContactAddress(contactId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.addresses(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContactAddress = (
  contactId: string,
  addressId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateContactAddressRequest) =>
      contactsApi.updateContactAddress(contactId, addressId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.addresses(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContactAddress = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (addressId: string) =>
      contactsApi.deleteContactAddress(contactId, addressId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.addresses(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Contact Notes ---

export const useContactNotes = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.notes(contactId),
    queryFn: () => contactsApi.getContactNotes(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContactNoteRequest) =>
      contactsApi.createContactNote(contactId, data),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Note created successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_NOTE_CREATED, {
        interactionType: variables.interactionType,
        hasFollowUp: !!variables.followUpDate,
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContactNote = (contactId: string, noteId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateContactNoteRequest) =>
      contactsApi.updateContactNote(contactId, noteId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Note updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (noteId: string) =>
      contactsApi.deleteContactNote(contactId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Note deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePinContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (noteId: string) =>
      contactsApi.pinContactNote(contactId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUnpinContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (noteId: string) =>
      contactsApi.unpinContactNote(contactId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Contact Relationships ---

export const useContactRelationships = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.relationships(contactId),
    queryFn: () => contactsApi.getContactRelationships(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactRelationship = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContactRelationshipRequest) =>
      contactsApi.createContactRelationship(contactId, data),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.relationships(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Relationship created successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_RELATIONSHIP_CREATED, {
        relationshipType: variables.relationshipType,
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateContactRelationship = (
  contactId: string,
  relationshipId: string
) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateContactRelationshipRequest) =>
      contactsApi.updateContactRelationship(contactId, relationshipId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.relationships(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Relationship updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteContactRelationship = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (relationshipId: string) =>
      contactsApi.deleteContactRelationship(contactId, relationshipId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.relationships(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Relationship deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Contact Tags ---

export const useAddContactTag = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: AddContactTagRequest) =>
      contactsApi.addContactTag(contactId, data),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Tag added successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_TAG_ADDED, { tag: variables.tag });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useRemoveContactTag = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (tag: string) => contactsApi.removeContactTag(contactId, tag),
    onSuccess: (_result, removedTag) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.activity(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(contactId),
      });
      showToast('Tag removed successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_TAG_REMOVED, { tag: removedTag });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Contact Activity ---

export const useContactActivity = (
  contactId: string | undefined,
  params?: PageParams
) => {
  return useQuery({
    queryKey: queryKeys.contacts.activity(contactId, params),
    queryFn: () => contactsApi.getContactActivity(contactId ?? '', params),
    enabled: !!contactId,
    placeholderData: keepPreviousData,
  });
};

// --- Duplicate Check ---

export const useCheckContactDuplicates = () => {
  return useMutation<DuplicateCheckResponse, Error, DuplicateCheckRequest>({
    mutationFn: (data) => contactsApi.checkContactDuplicates(data),
  });
};

// --- GDPR Erase ---

export const useEraseContactData = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (contactId: string) => contactsApi.eraseContactData(contactId),
    onSuccess: (_data, contactId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.detail(contactId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      showToast('Contact data erased', 'success');
      trackEvent(AnalyticsEvent.CONTACT_DATA_ERASED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
