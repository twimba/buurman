import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as tenantsApi from '../api/tenants';
import {
  CreateTenantRequest,
  UpdateTenantRequest,
  LinkTenantToPropertyRequest,
} from '../types/tenant';

export const useTenants = (search?: string) => {
  return useQuery({
    queryKey: ['tenants', search],
    queryFn: () => tenantsApi.getTenants(search),
  });
};

export const useTenant = (id: string | undefined) => {
  return useQuery({
    queryKey: ['tenant', id],
    queryFn: () => tenantsApi.getTenant(id!),
    enabled: !!id,
  });
};

export const useCreateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTenantRequest) => tenantsApi.createTenant(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const useUpdateTenant = (id: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateTenantRequest) =>
      tenantsApi.updateTenant(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const useDeleteTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tenantsApi.deleteTenant(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
};

export const useLinkTenantToProperty = (tenantId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: LinkTenantToPropertyRequest) =>
      tenantsApi.linkTenantToProperty(tenantId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
  });
};

export const useUnlinkTenantFromProperty = (tenantId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => tenantsApi.unlinkTenantFromProperty(tenantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
  });
};

export const useTenantHistory = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantHistory', tenantId],
    queryFn: () => tenantsApi.getTenantHistory(tenantId!),
    enabled: !!tenantId,
  });
};

export const useTenantDocuments = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantDocuments', tenantId],
    queryFn: () => tenantsApi.getTenantDocuments(tenantId!),
    enabled: !!tenantId,
  });
};

export const useTenantPhotos = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantPhotos', tenantId],
    queryFn: () => tenantsApi.getTenantPhotos(tenantId!),
    enabled: !!tenantId,
  });
};

export const useUploadTenantDocument = (tenantId: string) => {
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
    }) => tenantsApi.uploadTenantDocument(tenantId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantDocuments', tenantId] });
    },
  });
};

export const useUploadTenantPhoto = (tenantId: string) => {
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
    }) => tenantsApi.uploadTenantPhoto(tenantId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
    },
  });
};

export const useSetTenantMainPhoto = (tenantId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (photoId: string) =>
      tenantsApi.setTenantMainPhoto(tenantId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

export const useDeleteTenantDocument = (tenantId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) =>
      tenantsApi.deleteTenantDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantDocuments', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
    },
  });
};
