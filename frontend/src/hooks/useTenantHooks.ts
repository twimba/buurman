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
