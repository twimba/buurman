import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listUnits,
  getUnit,
  createUnit,
  bulkCreateUnits,
  updateUnit,
  deleteUnit,
  getUnitResidentialDetails,
  updateUnitResidentialDetails,
  getUnitAmenities,
  updateUnitAmenities,
} from '../generated/api/units/units';
import type {
  CreateUnitRequest,
  UpdateUnitRequest,
  BulkCreateUnitsRequest,
  UpdateUnitResidentialDetailsRequest,
  UpdateUnitAmenitiesRequest,
  UnitIdentifier,
  PropertyIdentifier,
} from '../types/unit';
import { queryKeys } from '../lib/queryKeys';

export const useUnits = (propertyIdentifier: PropertyIdentifier | undefined) =>
  useQuery({
    queryKey: queryKeys.units.all(propertyIdentifier),
    queryFn: () => listUnits(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });

export const useUnit = (unitIdentifier: UnitIdentifier | undefined) =>
  useQuery({
    queryKey: queryKeys.units.detail(unitIdentifier),
    queryFn: () => getUnit(unitIdentifier as UnitIdentifier),
    enabled: !!unitIdentifier,
  });

/**
 * Invalidation is deliberately broad: adding or removing a unit changes the
 * parent property's unitCount (which drives whether the Units tab exists at
 * all), its occupancy figures, the property list badges and dashboard stats.
 */
const invalidateUnitScope = (
  queryClient: ReturnType<typeof useQueryClient>,
  propertyIdentifier: PropertyIdentifier
) => {
  queryClient.invalidateQueries({
    queryKey: queryKeys.units.all(propertyIdentifier),
  });
  queryClient.invalidateQueries({
    queryKey: queryKeys.properties.detail(propertyIdentifier),
  });
  queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
  queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.stats() });
};

export const useCreateUnit = (propertyIdentifier: PropertyIdentifier) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit created',
    mutationFn: (data: CreateUnitRequest) =>
      createUnit(propertyIdentifier, data),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};

export const useBulkCreateUnits = (propertyIdentifier: PropertyIdentifier) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Units created',
    mutationFn: (data: BulkCreateUnitsRequest) =>
      bulkCreateUnits(propertyIdentifier, data),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};

export const useUpdateUnit = (
  propertyIdentifier: PropertyIdentifier,
  unitIdentifier: UnitIdentifier
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit updated',
    mutationFn: (data: UpdateUnitRequest) => updateUnit(unitIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.detail(unitIdentifier),
      });
      invalidateUnitScope(queryClient, propertyIdentifier);
    },
  });
};

export const useDeleteUnit = (propertyIdentifier: PropertyIdentifier) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit deleted',
    mutationFn: (unitIdentifier: UnitIdentifier) => deleteUnit(unitIdentifier),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};

export const useUnitResidentialDetails = (
  unitIdentifier: UnitIdentifier | undefined
) =>
  useQuery({
    queryKey: queryKeys.units.residentialDetails(unitIdentifier),
    queryFn: () =>
      getUnitResidentialDetails(unitIdentifier as UnitIdentifier),
    enabled: !!unitIdentifier,
  });

export const useUpdateUnitResidentialDetails = (
  unitIdentifier: UnitIdentifier
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Residential details updated',
    mutationFn: (data: UpdateUnitResidentialDetailsRequest) =>
      updateUnitResidentialDetails(unitIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.residentialDetails(unitIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.detail(unitIdentifier),
      });
    },
  });
};

export const useUnitAmenities = (unitIdentifier: UnitIdentifier | undefined) =>
  useQuery({
    queryKey: queryKeys.units.amenities(unitIdentifier),
    queryFn: () => getUnitAmenities(unitIdentifier as UnitIdentifier),
    enabled: !!unitIdentifier,
  });

export const useUpdateUnitAmenities = (unitIdentifier: UnitIdentifier) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Amenities updated',
    mutationFn: (data: UpdateUnitAmenitiesRequest) =>
      updateUnitAmenities(unitIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.amenities(unitIdentifier),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.detail(unitIdentifier),
      });
    },
  });
};
