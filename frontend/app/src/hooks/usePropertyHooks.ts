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
import { useToast } from '@buurman/ui';
import { getErrorMessage } from '../utils/errorMessages';
import { trackEvent } from '../utils/analytics';
import { AnalyticsEvent } from '../constants/analyticsEvents';
import { queryKeys } from '../lib/queryKeys';

export const useProperties = (
  params?: {
    status?: PropertyStatus;
    category?: PropertyCategory;
    query?: string;
  } & PageParams
) => {
  return useQuery({
    queryKey: queryKeys.properties.all(params),
    queryFn: () => propertiesApi.getProperties(params),
    placeholderData: keepPreviousData,
  });
};

export const useProperty = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.detail(id),
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
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Property created successfully', 'success');
      trackEvent(AnalyticsEvent.PROPERTY_CREATED);
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
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
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
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.stats(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.dashboard.propertyDashboard(),
      });
      showToast('Property deleted successfully', 'success');
      trackEvent(AnalyticsEvent.PROPERTY_DELETED);
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};

export const usePropertyDocuments = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.documents(propertyId),
    queryFn: () => propertiesApi.getPropertyDocuments(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyAuditLog = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.auditLog(propertyId),
    queryFn: () => propertiesApi.getPropertyAuditLog(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyPhotos = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.photos(propertyId),
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
        queryKey: queryKeys.properties.photos(propertyId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
      trackEvent(AnalyticsEvent.PHOTO_UPLOADED, { entity: 'property' });
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
        queryKey: queryKeys.properties.photos(propertyId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all() });
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
        queryKey: queryKeys.properties.documents(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.documents.all(),
      });
      trackEvent(AnalyticsEvent.DOCUMENT_UPLOADED, { entity: 'property' });
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
        queryKey: queryKeys.properties.documents(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.documents.all(),
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
    queryKey: queryKeys.properties.outdoorAreas(propertyId),
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
        queryKey: queryKeys.properties.outdoorAreas(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
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
        queryKey: queryKeys.properties.outdoorAreas(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
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
        queryKey: queryKeys.properties.outdoorAreas(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
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
    queryKey: queryKeys.properties.amenitiesCatalog(category),
    queryFn: () => propertiesApi.getAmenities(category),
    staleTime: Infinity,
  });
};

export const usePropertyAmenities = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.amenities(propertyId),
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
        queryKey: queryKeys.properties.amenities(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
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
    queryKey: queryKeys.properties.dashboard(propertyId, months),
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
        queryKey: queryKeys.properties.amenities(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.detail(propertyId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.properties.auditLog(propertyId),
      });
    },
    onError: (error) => {
      showToast(getErrorMessage(error), 'error');
    },
  });
};
