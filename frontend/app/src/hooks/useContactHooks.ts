import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getContacts,
  getContact,
  createContact,
  updateContact,
  deleteContact,
  getContactHistory,
  getContactAuditLog,
  getContactDocuments,
  getContactPhotos,
  uploadContactDocument,
  uploadContactPhoto,
  setContactMainPhoto,
  deleteContactDocument,
  getContactAddresses,
  createContactAddress,
  updateContactAddress,
  deleteContactAddress,
  getContactNotes,
  createContactNote,
  getContactCredits,
  createContactCredit,
  refundContactCredit,
  updateContactNote,
  deleteContactNote,
  pinContactNote,
  unpinContactNote,
  getContactRelationships,
  createContactRelationship,
  updateContactRelationship,
  deleteContactRelationship,
  addContactTag,
  removeContactTag,
  getContactActivity,
  checkContactDuplicates,
  eraseContactData,
} from '../generated/api/contacts/contacts';
import type {
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
  GetContactsParams,
  GetContactActivityParams,
  CreateContactCreditRequest,
  RefundContactCreditRequest,
} from '../generated/models';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
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
    queryFn: () => getContacts(params as GetContactsParams),
    placeholderData: keepPreviousData,
  });
};

export const useContact = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.detail(id),
    queryFn: () => getContact(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateContact = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreateContactRequest) => createContact(data),
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
  });
};

export const useUpdateContact = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Contact updated successfully',
    mutationFn: (data: UpdateContactRequest) => updateContact(id, data),
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
    },
  });
};

export const useDeleteContact = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (id: string) => deleteContact(id),
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
  });
};

export const useContactHistory = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.history(contactId),
    queryFn: () => getContactHistory(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactAuditLog = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.auditLog(contactId),
    queryFn: () => getContactAuditLog(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactDocuments = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.documents(contactId),
    queryFn: () => getContactDocuments(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactPhotos = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.photos(contactId),
    queryFn: () => getContactPhotos(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useUploadContactDocument = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadContactDocument(contactId, { file }, { title, notes }),
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
  });
};

export const useUploadContactPhoto = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadContactPhoto(contactId, { file }, { title, notes }),
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
  });
};

export const useSetContactMainPhoto = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (photoId: string) => setContactMainPhoto(contactId, photoId),
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
  });
};

export const useDeleteContactDocument = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (documentId: string) => deleteContactDocument(documentId),
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
  });
};

export const useContactAddresses = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.addresses(contactId),
    queryFn: () => getContactAddresses(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactAddress = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateContactAddressRequest) =>
      createContactAddress(contactId, data),
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
  });
};

export const useUpdateContactAddress = (
  contactId: string,
  addressId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: UpdateContactAddressRequest) =>
      updateContactAddress(contactId, addressId, data),
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
  });
};

export const useDeleteContactAddress = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (addressId: string) =>
      deleteContactAddress(contactId, addressId),
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
  });
};

// --- Contact Notes ---

export const useContactNotes = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.notes(contactId),
    queryFn: () => getContactNotes(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreateContactNoteRequest) =>
      createContactNote(contactId, data),
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
  });
};

export const useUpdateContactNote = (contactId: string, noteId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Note updated successfully',
    mutationFn: (data: UpdateContactNoteRequest) =>
      updateContactNote(contactId, noteId, data),
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
    },
  });
};

export const useDeleteContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Note deleted successfully',
    mutationFn: (noteId: string) => deleteContactNote(contactId, noteId),
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
    },
  });
};

export const usePinContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (noteId: string) => pinContactNote(contactId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
    },
  });
};

export const useUnpinContactNote = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (noteId: string) => unpinContactNote(contactId, noteId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.notes(contactId),
      });
    },
  });
};

// --- Contact Relationships ---

export const useContactRelationships = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.relationships(contactId),
    queryFn: () => getContactRelationships(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactRelationship = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreateContactRelationshipRequest) =>
      createContactRelationship(contactId, data),
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
  });
};

export const useUpdateContactRelationship = (
  contactId: string,
  relationshipId: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Relationship updated successfully',
    mutationFn: (data: UpdateContactRelationshipRequest) =>
      updateContactRelationship(contactId, relationshipId, data),
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
    },
  });
};

export const useDeleteContactRelationship = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Relationship deleted successfully',
    mutationFn: (relationshipId: string) =>
      deleteContactRelationship(contactId, relationshipId),
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
    },
  });
};

// --- Contact Tags ---

export const useAddContactTag = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: AddContactTagRequest) => addContactTag(contactId, data),
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
  });
};

export const useRemoveContactTag = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (tag: string) => removeContactTag(contactId, tag),
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
  });
};

// --- Contact Activity ---

export const useContactActivity = (
  contactId: string | undefined,
  params?: PageParams
) => {
  return useQuery({
    queryKey: queryKeys.contacts.activity(contactId, params),
    queryFn: () =>
      getContactActivity(contactId ?? '', params as GetContactActivityParams),
    enabled: !!contactId,
    placeholderData: keepPreviousData,
  });
};

// --- Duplicate Check ---

export const useCheckContactDuplicates = () => {
  return useMutation<DuplicateCheckResponse, Error, DuplicateCheckRequest>({
    mutationFn: (data) => checkContactDuplicates(data),
  });
};

// --- GDPR Erase ---

export const useEraseContactData = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (contactId: string) => eraseContactData(contactId),
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
  });
};

// --- Credits (BUUR-101) ---

export const useContactCredits = (contactId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.contacts.credits(contactId),
    queryFn: () => getContactCredits(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useCreateContactCredit = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: CreateContactCreditRequest) =>
      createContactCredit(contactId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.credits(contactId),
      });
      trackEvent(AnalyticsEvent.CREDIT_NOTE_CREATED);
    },
  });
};

export const useRefundContactCredit = (contactId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      creditId,
      data,
    }: {
      creditId: string;
      data: RefundContactCreditRequest;
    }) => refundContactCredit(contactId, creditId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.credits(contactId),
      });
      trackEvent(AnalyticsEvent.CREDIT_REFUNDED);
    },
  });
};
