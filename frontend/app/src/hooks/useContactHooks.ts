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
  LinkContactToPropertyRequest,
  CreateContactAddressRequest,
  UpdateContactAddressRequest,
} from '../types/contact';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

export const useContacts = (params?: { search?: string } & PageParams) => {
  return useQuery({
    queryKey: ['contacts', params],
    queryFn: () => contactsApi.getContacts(params),
    placeholderData: keepPreviousData,
  });
};

export const useContact = (id: string | undefined) => {
  return useQuery({
    queryKey: ['contact', id],
    queryFn: () => contactsApi.getContact(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateContact = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateContactRequest) =>
      contactsApi.createContact(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['contact', id] });
      queryClient.invalidateQueries({ queryKey: ['contactAuditLog', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
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
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Contact deleted successfully', 'success');
      trackEvent(AnalyticsEvent.CONTACT_DELETED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useLinkContactToProperty = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: LinkContactToPropertyRequest) =>
      contactsApi.linkContactToProperty(contactId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUnlinkContactFromProperty = (contactId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => contactsApi.unlinkContactFromProperty(contactId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContactHistory = (contactId: string | undefined) => {
  return useQuery({
    queryKey: ['contactHistory', contactId],
    queryFn: () => contactsApi.getContactHistory(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactAuditLog = (contactId: string | undefined) => {
  return useQuery({
    queryKey: ['contactAuditLog', contactId],
    queryFn: () => contactsApi.getContactAuditLog(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactDocuments = (contactId: string | undefined) => {
  return useQuery({
    queryKey: ['contactDocuments', contactId],
    queryFn: () => contactsApi.getContactDocuments(contactId ?? ''),
    enabled: !!contactId,
  });
};

export const useContactPhotos = (contactId: string | undefined) => {
  return useQuery({
    queryKey: ['contactPhotos', contactId],
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
        queryKey: ['contactDocuments', contactId],
      });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({
        queryKey: ['documents'],
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
        queryKey: ['contactPhotos', contactId],
      });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
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
        queryKey: ['contactPhotos', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contacts'] });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
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
        queryKey: ['contactDocuments', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
      queryClient.invalidateQueries({
        queryKey: ['documents'],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useContactAddresses = (contactId: string | undefined) => {
  return useQuery({
    queryKey: ['contactAddresses', contactId],
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
        queryKey: ['contactAddresses', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
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
        queryKey: ['contactAddresses', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
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
        queryKey: ['contactAddresses', contactId],
      });
      queryClient.invalidateQueries({ queryKey: ['contact', contactId] });
      queryClient.invalidateQueries({
        queryKey: ['contactAuditLog', contactId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
