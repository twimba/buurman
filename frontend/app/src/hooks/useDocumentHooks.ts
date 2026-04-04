import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as documentsApi from '../api/documents';
import { SearchDocumentsParams } from '../api/documents';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';
import { queryKeys } from '../lib/queryKeys';

export const useDocuments = (params?: SearchDocumentsParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.documents.all(params),
    queryFn: () => documentsApi.searchDocuments(params),
    placeholderData: keepPreviousData,
  });
};

export const useDocument = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.documents.detail(id),
    queryFn: () => documentsApi.getDocument(id ?? ''),
    enabled: !!id,
  });
};

export const useDeleteDocument = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.all() });
      showToast('Document deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useBulkDownload = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentIds: string[]) =>
      documentsApi.bulkDownloadDocuments(documentIds),
    onSuccess: (blob) => {
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'documents.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      showToast('Documents downloaded successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateDocument = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: { title: string | null; notes: string | null };
    }) => documentsApi.updateDocument(id, data),
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
      showToast('Document updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
