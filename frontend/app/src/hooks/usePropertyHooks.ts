import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import * as propertiesApi from '../api/properties';
import {
  CreatePropertyRequest,
  UpdatePropertyRequest,
  PropertyStatus,
  PropertyCategory,
  OutdoorAreaRequest,
} from '../types/property';
import type { PageParams } from '@/types/common';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessages';

export const useProperties = (
  params?: {
    status?: PropertyStatus;
    category?: PropertyCategory;
    query?: string;
  } & PageParams
) => {
  return useQuery({
    queryKey: ['properties', params],
    queryFn: () => propertiesApi.getProperties(params),
    placeholderData: keepPreviousData,
  });
};

export const useProperty = (id: string | undefined) => {
  return useQuery({
    queryKey: ['property', id],
    queryFn: () => propertiesApi.getProperty(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateProperty = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: CreatePropertyRequest) =>
      propertiesApi.createProperty(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Property created successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateProperty = (id: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: UpdatePropertyRequest) =>
      propertiesApi.updateProperty(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', id] });
      queryClient.invalidateQueries({ queryKey: ['propertyAuditLog', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Property updated successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteProperty = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (id: string) => propertiesApi.deleteProperty(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['propertyDashboard'] });
      showToast('Property deleted successfully', 'success');
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePropertyDocuments = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyDocuments', propertyId],
    queryFn: () => propertiesApi.getPropertyDocuments(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyAuditLog = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyAuditLog', propertyId],
    queryFn: () => propertiesApi.getPropertyAuditLog(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyPhotos = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['propertyPhotos', propertyId],
    queryFn: () => propertiesApi.getPropertyPhotos(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useUploadPropertyPhoto = (propertyId: string) => {
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
    }) => propertiesApi.uploadPropertyPhoto(propertyId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyPhotos', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useSetMainPhoto = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (photoId: string) =>
      propertiesApi.setMainPhoto(propertyId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyPhotos', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['photos'] });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUploadPropertyDocument = (propertyId: string) => {
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
    }) => propertiesApi.uploadPropertyDocument(propertyId, file, title, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyDocuments', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
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

export const useDeleteDocument = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (documentId: string) =>
      propertiesApi.deleteDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['propertyDocuments', propertyId],
      });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
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

// --- Outdoor Areas ---

export const useOutdoorAreas = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['outdoor-areas', propertyId],
    queryFn: () => propertiesApi.getOutdoorAreas(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useCreateOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (data: OutdoorAreaRequest) =>
      propertiesApi.createOutdoorArea(propertyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['outdoor-areas', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useUpdateOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      areaId,
      data,
    }: {
      areaId: string;
      data: OutdoorAreaRequest;
    }) => propertiesApi.updateOutdoorArea(propertyId, areaId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['outdoor-areas', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const useDeleteOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (areaId: string) =>
      propertiesApi.deleteOutdoorArea(propertyId, areaId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['outdoor-areas', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Amenities ---

export const useAmenities = (category?: string) => {
  return useQuery({
    queryKey: ['amenities', category],
    queryFn: () => propertiesApi.getAmenities(category),
    staleTime: Infinity,
  });
};

export const usePropertyAmenities = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: ['property-amenities', propertyId],
    queryFn: () => propertiesApi.getPropertyAmenities(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useAddPropertyAmenity = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: ({
      amenityIdentifier,
      notes,
    }: {
      amenityIdentifier: string;
      notes?: string | null;
    }) =>
      propertiesApi.addPropertyAmenity(propertyId, amenityIdentifier, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['property-amenities', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

// --- Property Dashboard ---

export const usePropertyDashboard = (
  propertyId: string | undefined,
  months?: number
) => {
  return useQuery({
    queryKey: ['propertyDashboard', propertyId, months],
    queryFn: () => propertiesApi.getPropertyDashboard(propertyId ?? '', months),
    enabled: !!propertyId,
    staleTime: 60_000,
  });
};

export const useRemovePropertyAmenity = (propertyId: string) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutation({
    mutationFn: (amenityIdentifier: string) =>
      propertiesApi.removePropertyAmenity(propertyId, amenityIdentifier),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['property-amenities', propertyId],
      });
      queryClient.invalidateQueries({ queryKey: ['property', propertyId] });
      queryClient.invalidateQueries({
        queryKey: ['propertyAuditLog', propertyId],
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
