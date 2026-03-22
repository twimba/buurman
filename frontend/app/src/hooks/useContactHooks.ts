import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as tenantsApi from '../api/tenants';
import {
  CreateTenantRequest,
  UpdateTenantRequest,
  LinkTenantToPropertyRequest,
  CreateTenantAddressRequest,
  UpdateTenantAddressRequest,
} from '../types/tenant';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';

export const useTenants = (params?: { search?: string } & PageParams) => {
  return useQuery({
    queryKey: ['tenants', params],
    queryFn: () => tenantsApi.getTenants(params),
    placeholderData: keepPreviousData,
  });
};

export const useTenant = (id: string | undefined) => {
  return useQuery({
    queryKey: ['tenant', id],
    queryFn: () => tenantsApi.getTenant(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateTenant = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateTenantRequest) => tenantsApi.createTenant(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tenant created successfully', 'success');
      trackEvent(AnalyticsEvent.TENANT_CREATED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateTenant = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateTenantRequest) =>
      tenantsApi.updateTenant(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', id] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tenant updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteTenant = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => tenantsApi.deleteTenant(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Tenant deleted successfully', 'success');
      trackEvent(AnalyticsEvent.TENANT_DELETED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useLinkTenantToProperty = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: LinkTenantToPropertyRequest) =>
      tenantsApi.linkTenantToProperty(tenantId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUnlinkTenantFromProperty = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: () => tenantsApi.unlinkTenantFromProperty(tenantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useTenantHistory = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantHistory', tenantId],
    queryFn: () => tenantsApi.getTenantHistory(tenantId ?? ''),
    enabled: !!tenantId,
  });
};

export const useTenantAuditLog = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantAuditLog', tenantId],
    queryFn: () => tenantsApi.getTenantAuditLog(tenantId ?? ''),
    enabled: !!tenantId,
  });
};

export const useTenantDocuments = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantDocuments', tenantId],
    queryFn: () => tenantsApi.getTenantDocuments(tenantId ?? ''),
    enabled: !!tenantId,
  });
};

export const useTenantPhotos = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantPhotos', tenantId],
    queryFn: () => tenantsApi.getTenantPhotos(tenantId ?? ''),
    enabled: !!tenantId,
  });
};

export const useUploadTenantDocument = (tenantId: string) => {
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
    }) => tenantsApi.uploadTenantDocument(tenantId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantDocuments', tenantId],
      });
      queryClient.invalidateQueries({
        queryKey: ['tenantAuditLog', tenantId],
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

export const useUploadTenantPhoto = (tenantId: string) => {
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
    }) => tenantsApi.uploadTenantPhoto(tenantId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos', tenantId] });
      queryClient.invalidateQueries({
        queryKey: ['tenantAuditLog', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useSetTenantMainPhoto = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (photoId: string) =>
      tenantsApi.setTenantMainPhoto(tenantId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenantPhotos', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteTenantDocument = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      tenantsApi.deleteTenantDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantDocuments', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({
        queryKey: ['tenantAuditLog', tenantId],
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

export const useTenantAddresses = (tenantId: string | undefined) => {
  return useQuery({
    queryKey: ['tenantAddresses', tenantId],
    queryFn: () => tenantsApi.getTenantAddresses(tenantId ?? ''),
    enabled: !!tenantId,
  });
};

export const useCreateTenantAddress = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreateTenantAddressRequest) =>
      tenantsApi.createTenantAddress(tenantId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantAddresses', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateTenantAddress = (tenantId: string, addressId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdateTenantAddressRequest) =>
      tenantsApi.updateTenantAddress(tenantId, addressId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantAddresses', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteTenantAddress = (tenantId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (addressId: string) =>
      tenantsApi.deleteTenantAddress(tenantId, addressId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tenantAddresses', tenantId],
      });
      queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
      queryClient.invalidateQueries({ queryKey: ['tenantAuditLog', tenantId] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
