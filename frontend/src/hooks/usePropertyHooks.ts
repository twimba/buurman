import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as propertiesApi from '../api/properties';
import {
  CreatePropertyRequest,
  UpdatePropertyRequest,
  PropertyStatus,
} from '../types/property';

export const useProperties = (status?: PropertyStatus) => {
  return useQuery({
    queryKey: ['properties', status],
    queryFn: () => propertiesApi.getProperties(status),
  });
};

export const useProperty = (id: string | undefined) => {
  return useQuery({
    queryKey: ['property', id],
    queryFn: () => propertiesApi.getProperty(id!),
    enabled: !!id,
  });
};

export const useCreateProperty = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePropertyRequest) =>
      propertiesApi.createProperty(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const useUpdateProperty = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdatePropertyRequest) =>
      propertiesApi.updateProperty(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', id] });
      queryClient.invalidateQueries({ queryKey: ['propertyAuditLog', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const useDeleteProperty = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => propertiesApi.deleteProperty(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const usePropertyDocuments = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyDocuments', propertyId],
    queryFn: () => propertiesApi.getPropertyDocuments(propertyId!),
    enabled: !!propertyId,
  });
};

export const usePropertyAuditLog = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyAuditLog', propertyId],
    queryFn: () => propertiesApi.getPropertyAuditLog(propertyId!),
    enabled: !!propertyId,
  });
};

export const usePropertyPhotos = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyPhotos', propertyId],
    queryFn: () => propertiesApi.getPropertyPhotos(propertyId!),
    enabled: !!propertyId,
  });
};

export const useUploadPropertyPhoto = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => propertiesApi.uploadPropertyPhoto(propertyId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyPhotos', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
    },
  });
};

export const useSetMainPhoto = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (photoId: string) =>
      propertiesApi.setMainPhoto(propertyId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyPhotos', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
    },
  });
};

export const useUploadPropertyDocument = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => propertiesApi.uploadPropertyDocument(propertyId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyDocuments', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
  });
};

export const useDeleteDocument = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) =>
      propertiesApi.deleteDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyDocuments', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyPhotos', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
  });
};
