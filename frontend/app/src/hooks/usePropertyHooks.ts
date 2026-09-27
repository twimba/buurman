import {
  useQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  getProperties,
  getProperty,
  createProperty,
  updateProperty,
  deleteProperty,
  getPropertyDocuments,
  getPropertyAuditLog,
  getPropertyPhotos,
  uploadPropertyPhoto,
  setMainPropertyPhoto,
  uploadPropertyDocument,
  deletePropertyDocument,
} from '../generated/api/properties/properties';
import {
  getOutdoorAreas,
  createOutdoorArea,
  updateOutdoorArea,
  deleteOutdoorArea,
} from '../generated/api/property-outdoor-areas/property-outdoor-areas';
import { getAllAmenities } from '../generated/api/amenities/amenities';
import { getDashboard } from '../generated/api/property-dashboard/property-dashboard';
import type { GetPropertiesParams } from '../generated/models';
import {
  CreatePropertyRequest,
  UpdatePropertyRequest,
  PropertyStatus,
  PropertyCategory,
  OutdoorAreaRequest,
} from '../types/property';
import type { PageParams } from '@/types/common';
import { useToast } from '@buurman/ui';
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
    queryFn: () => getProperties(params as GetPropertiesParams),
    placeholderData: keepPreviousData,
  });
};

export const useProperty = (id: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.detail(id),
    queryFn: () => getProperty(id ?? ''),
    enabled: !!id,
  });
};

export const useCreateProperty = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (data: CreatePropertyRequest) => createProperty(data),
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
  });
};

export const useUpdateProperty = (id: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Property updated successfully',
    mutationFn: (data: UpdatePropertyRequest) => updateProperty(id, data),
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
    },
  });
};

export const useDeleteProperty = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  return useMutationWithToast({
    mutationFn: (id: string) => deleteProperty(id),
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
  });
};

export const usePropertyDocuments = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.documents(propertyId),
    queryFn: () => getPropertyDocuments(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyAuditLog = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.auditLog(propertyId),
    queryFn: () => getPropertyAuditLog(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const usePropertyPhotos = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.photos(propertyId),
    queryFn: () => getPropertyPhotos(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useUploadPropertyPhoto = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadPropertyPhoto(propertyId, { file }, { title, notes }),
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
  });
};

export const useSetMainPhoto = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (photoId: string) => setMainPropertyPhoto(propertyId, photoId),
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
  });
};

export const useUploadPropertyDocument = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      file,
      title,
      notes,
    }: {
      file: File;
      title?: string;
      notes?: string;
    }) => uploadPropertyDocument(propertyId, { file }, { title, notes }),
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
  });
};

export const useDeleteDocument = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (documentId: string) => deletePropertyDocument(documentId),
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
  });
};

// --- Outdoor Areas ---

export const useOutdoorAreas = (propertyId: string | undefined) => {
  return useQuery({
    queryKey: queryKeys.properties.outdoorAreas(propertyId),
    queryFn: () => getOutdoorAreas(propertyId ?? ''),
    enabled: !!propertyId,
  });
};

export const useCreateOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (data: OutdoorAreaRequest) =>
      createOutdoorArea(propertyId, data),
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
  });
};

export const useUpdateOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: ({
      areaId,
      data,
    }: {
      areaId: string;
      data: OutdoorAreaRequest;
    }) => updateOutdoorArea(propertyId, areaId, data),
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
  });
};

export const useDeleteOutdoorArea = (propertyId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    mutationFn: (areaId: string) => deleteOutdoorArea(propertyId, areaId),
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
  });
};

// --- Amenities ---

export const useAmenities = (category?: string) => {
  return useQuery({
    queryKey: queryKeys.properties.amenitiesCatalog(category),
    queryFn: () => getAllAmenities(category ? { category } : undefined),
    staleTime: Infinity,
  });
};

// --- Property Dashboard ---

export const usePropertyDashboard = (
  propertyId: string | undefined,
  months?: number
) => {
  return useQuery({
    queryKey: queryKeys.properties.dashboard(propertyId, months),
    queryFn: () =>
      getDashboard(propertyId ?? '', months != null ? { months } : undefined),
    enabled: !!propertyId,
    staleTime: 60_000,
  });
};
