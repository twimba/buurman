import { useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getAllPhotos,
  getPhoto,
  deletePhoto,
  bulkDownload,
  updatePhoto,
} from '../generated/api/photos/photos';
import type { GetAllPhotosParams } from '../generated/models';
import type { PageParams } from '@/types/common';
import { queryKeys } from '../lib/queryKeys';
import { downloadBlob } from '../utils/downloadBlob';

export interface SearchPhotosParams {
  search?: string;
  entityType?: string;
}

export const usePhotos = (params?: SearchPhotosParams & PageParams) => {
  return useQuery({
    queryKey: queryKeys.photos.all(params),
    queryFn: () => getAllPhotos(params as GetAllPhotosParams),
    placeholderData: keepPreviousData,
  });
};

export const usePhoto = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.photos.detail(id),
    queryFn: () => getPhoto(id ?? ''),
    enabled: !!id,
  });
};

export const useDeletePhoto = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Photo deleted successfully',
    mutationFn: (id: string) => deletePhoto(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.photos(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.photos() });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
    },
  });
};

export const useBulkDownloadPhotos = () => {
  return useMutationWithToast({
    successMessage: 'Photos downloaded successfully',
    mutationFn: (photoIds: string[]) =>
      bulkDownload({ documentIdentifiers: photoIds }),
    onSuccess: (blob) => {
      downloadBlob(blob, 'photos.zip');
    },
  });
};

export const useUpdatePhoto = () => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Photo updated successfully',
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: { title: string | null; notes: string | null };
    }) =>
      updatePhoto(id, {
        title: data.title ?? undefined,
        notes: data.notes ?? undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.photos(),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contacts.photos() });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.contacts.auditLog(),
      });
    },
  });
};
