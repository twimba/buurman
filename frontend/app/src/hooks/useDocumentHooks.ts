import {
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getAllDocuments,
  getDocument,
  deleteDocument,
  bulkDownloadDocuments,
  updateDocument,
} from '../generated/api/documents/documents';
import type { GetAllDocumentsParams } from '../generated/models';
import type { PageParams } from '@/types/common';
import { queryKeys } from '../lib/queryKeys';
import { downloadBlob } from '../utils/downloadBlob';

export interface SearchDocumentsParams {
  search?: string;
  entityType?: string;
}

export const useDocuments = (params?: SearchDocumentsParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.documents.all(params),
    queryFn: () => getAllDocuments(params as GetAllDocumentsParams),
    placeholderData: keepPreviousData,
  });
};

export const useDocument = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.documents.detail(id),
    queryFn: () => getDocument(id ?? ''),
    enabled: !!id,
  });
};

export const useDeleteDocument = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document deleted successfully',
    mutationFn: (id: string) => deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
    },
  });
};

export const useBulkDownload = () => {
  return useMutationWithToast({
    successMessage: 'Documents downloaded successfully',
    mutationFn: (documentIds: string[]) =>
      bulkDownloadDocuments({ documentIdentifiers: documentIds }),
    onSuccess: (blob) => {
      downloadBlob(blob, 'documents.zip');
    },
  });
};

export const useUpdateDocument = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Document updated successfully',
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: { title: string | null; notes: string | null };
    }) =>
      updateDocument(id, {
        title: data.title ?? undefined,
        notes: data.notes ?? undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.documents(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.documents(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(),
      });
    },
  });
};
