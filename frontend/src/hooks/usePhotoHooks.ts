import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as photosApi from '../api/photos';
import { SearchPhotosParams } from '../api/photos';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const usePhotos = (params?: SearchPhotosParams & PageParams) => {
  return useQuery({
    queryKey: ['photos', params],
    queryFn: () => photosApi.searchPhotos(params),
    placeholderData: keepPreviousData,
  });
};

export const usePhoto = (id: string | undefined) => {
  return useQuery({
    queryKey: ['photo', id],
    queryFn: () => photosApi.getPhoto(id!),
    enabled: !!id,
  });
};

export const useDeletePhoto = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => photosApi.deletePhoto(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photos'] });
      queryClient.invalidateQueries({ queryKey: ['propertyPhotos'] });
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      showToast('Photo deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useBulkDownloadPhotos = () => {
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (photoIds: string[]) => photosApi.bulkDownloadPhotos(photoIds),
    onSuccess: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'photos.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      showToast('Photos downloaded successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdatePhoto = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: { title: string | null; notes: string | null };
    }) => photosApi.updatePhoto(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['photos'] });
      queryClient.invalidateQueries({ queryKey: ['propertyPhotos'] });
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['propertyAuditLog'] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog'] });
      showToast('Photo updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
