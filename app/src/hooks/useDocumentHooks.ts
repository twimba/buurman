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

export const useDocuments = (params?: SearchDocumentsParams & PageParams) => {
  return useQuery({
    queryKey: ['documents', params],
    queryFn: () => documentsApi.searchDocuments(params),
    placeholderData: keepPreviousData,
  });
};

export const useDocument = (id: string | undefined) => {
  return useQuery({
    queryKey: ['document', id],
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
      queryClient.invalidateQueries({ queryKey: ['documents'] });
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
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['tenantDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['propertyAuditLog'] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog'] });
      showToast('Document updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
